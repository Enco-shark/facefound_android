package com.Enco.facefound.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import com.Enco.facefound.ml.OnnxFaceRecognition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

/**
 * 视频人脸识别处理器
 * 参考 video.py 的实现逻辑：
 * - 批量帧处理
 * - 绿色框 + 人名标注（video.py 绘制风格）
 * - 输出处理后的视频文件
 */
class VideoProcessor(private val faceRecognizer: OnnxFaceRecognition) {

    companion object {
        private const val TAG = "VideoProcessor"
        private const val FRAME_SAMPLE_INTERVAL_US = 100_000L
        private const val TIMEOUT_US = 10_000L
        private const val OUTPUT_MIME = "video/avc"
        private const val OUTPUT_BITRATE = 4_000_000
        private const val OUTPUT_FRAME_RATE = 15
        private const val OUTPUT_I_FRAME_INTERVAL = 2
        private const val COLOR_GREEN = 0xFF00FF00.toInt()
        private const val COLOR_YELLOW = 0xFFFFFF00.toInt()
        private const val COLOR_BG = 0xA0000000.toInt()
    }

    data class VideoInfo(
        val durationMs: Long,
        val totalFrames: Int,
        val width: Int,
        val height: Int,
        val rotation: Int,
        val mimeType: String
    )

    data class ProcessedFrame(
        val frameIndex: Int,
        val presentationTimeUs: Long,
        val bitmap: Bitmap,
        val detections: List<OnnxFaceRecognition.FaceDetection>,
        val names: List<String>
    )

    data class ProcessProgress(
        val frameIndex: Int,
        val totalFrames: Int,
        val detections: List<OnnxFaceRecognition.FaceDetection>,
        val names: List<String>
    )

    fun getVideoInfo(context: Context, videoUri: Uri): VideoInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/avc"
            val width = widthStr?.toIntOrNull() ?: 1920
            val height = heightStr?.toIntOrNull() ?: 1080
            val rotation = rotationStr?.toIntOrNull() ?: 0
            val totalFrames = if (durationMs > 0) ((durationMs / 1000f) * 30).toInt() else 0

            VideoInfo(durationMs, totalFrames, width, height, rotation, mimeType)
        } catch (e: Exception) {
            Log.e(TAG, "获取视频信息失败: ${e.message}")
            VideoInfo(0L, 0, 1920, 1080, 0, "video/avc")
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun extractFramesWithRetriever(
        context: Context,
        videoUri: Uri
    ): Flow<Pair<Int, Bitmap>> = flow {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            if (durationMs <= 0) return@flow

            var timeUs = 0L
            var frameIndex = 0
            val intervalUs = FRAME_SAMPLE_INTERVAL_US

            while (timeUs <= durationMs * 1000) {
                if (!coroutineContext.isActive) break
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bitmap != null) {
                    emit(frameIndex to bitmap)
                }
                frameIndex++
                timeUs += intervalUs
            }
        } catch (e: Exception) {
            Log.e(TAG, "提取帧失败: ${e.message}")
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    fun processVideoFrames(
        context: Context,
        videoUri: Uri,
        templates: Map<String, FloatArray>,
        threshold: Float,
        onProgress: (ProcessProgress) -> Unit
    ): Flow<ProcessedFrame> = flow {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            if (durationMs <= 0) {
                Log.e(TAG, "无法获取视频时长")
                return@flow
            }

            var timeUs = 0L
            var frameIndex = 0
            val intervalUs = FRAME_SAMPLE_INTERVAL_US

            while (timeUs <= durationMs * 1000) {
                if (!coroutineContext.isActive) break

                val frameBitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frameBitmap == null) {
                    frameIndex++
                    timeUs += intervalUs
                    continue
                }

                val detections = faceRecognizer.detectFaces(frameBitmap)

                val names = mutableListOf<String>()
                detections.forEach { detection ->
                    val faceBitmap = faceRecognizer.alignFace(frameBitmap, detection)
                    if (faceBitmap != null) {
                        val result = faceRecognizer.recognizeFace(faceBitmap, templates, threshold)
                        names.add(result.name)
                        faceBitmap.recycle()
                    } else {
                        names.add("Face")
                    }
                }

                val drawnBitmap = drawVideoResultsInPlace(frameBitmap, detections, names)

                onProgress(ProcessProgress(frameIndex, 0, detections, names))

                emit(ProcessedFrame(
                    frameIndex = frameIndex,
                    presentationTimeUs = timeUs,
                    bitmap = drawnBitmap,
                    detections = detections,
                    names = names
                ))

                frameIndex++
                timeUs += intervalUs
            }
        } catch (e: Exception) {
            Log.e(TAG, "视频帧处理失败: ${e.message}", e)
            throw e
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    suspend fun encodeToVideo(
        frames: List<ProcessedFrame>,
        outputFile: File,
        width: Int,
        height: Int,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (frames.isEmpty()) throw IllegalStateException("没有可编码的帧")

        val outputWidth = if (width > 0) width else frames.firstOrNull()?.bitmap?.width ?: 1920
        val outputHeight = if (height > 0) height else frames.firstOrNull()?.bitmap?.height ?: 1080

        val alignedWidth = (outputWidth / 16) * 16
        val alignedHeight = (outputHeight / 16) * 16

        val muxer = MediaMuxer(
            outputFile.absolutePath,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )

        val format = MediaFormat.createVideoFormat(OUTPUT_MIME, alignedWidth, alignedHeight).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_BITRATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_I_FRAME_INTERVAL)
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
        }

        val codecName = findEncoderForMimeType(OUTPUT_MIME)
            ?: throw IllegalStateException("找不到 H.264 编码器")
        val encoder = MediaCodec.createByCodecName(codecName)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val totalFrames = frames.size
        var muxerStarted = false
        var trackIndex = -1
        var frameIdx = 0

        try {
            val tempBitmap = Bitmap.createBitmap(alignedWidth, alignedHeight, Bitmap.Config.ARGB_8888)
            val tempCanvas = Canvas(tempBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val pixelBuffer = IntArray(alignedWidth * alignedHeight)
            val totalSize = alignedWidth * alignedHeight * 3 / 2

            for (frame in frames) {
                if (!coroutineContext.isActive) break

                val srcBitmap = frame.bitmap
                if (!srcBitmap.isRecycled) {
                    tempCanvas.drawColor(Color.BLACK)
                    val scale = min(
                        alignedWidth.toFloat() / srcBitmap.width,
                        alignedHeight.toFloat() / srcBitmap.height
                    )
                    val scaledW = (srcBitmap.width * scale).toInt()
                    val scaledH = (srcBitmap.height * scale).toInt()
                    val left = (alignedWidth - scaledW) / 2
                    val top = (alignedHeight - scaledH) / 2
                    tempCanvas.drawBitmap(srcBitmap, null, Rect(left, top, left + scaledW, top + scaledH), paint)
                }

                val inputIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                if (inputIndex >= 0) {
                    val buffer = encoder.getInputBuffer(inputIndex)!!
                    buffer.clear()

                    tempBitmap.getPixels(pixelBuffer, 0, alignedWidth, 0, 0, alignedWidth, alignedHeight)

                    if (buffer.capacity() >= totalSize) {
                        convertARGBToNV12(pixelBuffer, buffer, alignedWidth, alignedHeight)

                        val pts = frame.presentationTimeUs.coerceAtLeast(
                            (frameIdx * 1_000_000L / OUTPUT_FRAME_RATE)
                        )

                        encoder.queueInputBuffer(inputIndex, 0, totalSize, pts, 0)
                        frameIdx++
                    } else {
                        encoder.queueInputBuffer(inputIndex, 0, 0, 0, 0)
                    }
                }

                onProgress(frameIdx.toFloat() / totalFrames)
            }

            val eosIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
            if (eosIndex >= 0) {
                encoder.queueInputBuffer(eosIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

            drainEncoderFinal(encoder, muxer) { started, idx ->
                muxerStarted = started
                trackIndex = idx
            }

            tempBitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "编码视频失败: ${e.message}", e)
            throw e
        } finally {
            try { encoder.stop() } catch (_: Exception) {}
            try { encoder.release() } catch (_: Exception) {}
            try {
                if (muxerStarted) muxer.stop()
                muxer.release()
            } catch (_: Exception) {}
        }

        onProgress(1f)
        outputFile
    }

    /**
     * 将 ARGB_8888 像素转换为 NV12 (YUV420 semi-planar) 格式
     * NV12 布局: [Y平面 (W*H)] [UV交错 (W*H/2)]
     */
    private fun convertARGBToNV12(
        pixels: IntArray,
        outputBuffer: ByteBuffer,
        width: Int,
        height: Int
    ) {
        val yPlaneSize = width * height

        var yIdx = 0
        var uvIdx = yPlaneSize

        var py = 0
        while (py < height) {
            val rowOffset = py * width
            var px = 0
            while (px < width) {
                val pixel = pixels[rowOffset + px]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                outputBuffer.put(yIdx, yVal.coerceIn(0, 255).toByte())
                yIdx++

                if ((py and 1) == 0 && (px and 1) == 0) {
                    val uVal = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val vVal = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    outputBuffer.put(uvIdx, uVal.coerceIn(0, 255).toByte())
                    outputBuffer.put(uvIdx + 1, vVal.coerceIn(0, 255).toByte())
                    uvIdx += 2
                }
                px++
            }
            py++
        }
    }

    /**
     * 从编码器排出已编码数据并写入 muxer
     */
    private fun drainEncoderFinal(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        onFormatChanged: (Boolean, Int) -> Unit
    ) {
        val bufferInfo = MediaCodec.BufferInfo()
        var muxerStarted = false
        var trackIndex = -1

        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                        onFormatChanged(true, trackIndex)
                    }
                }
                outputIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputIndex)!!
                    if (bufferInfo.size > 0 && muxerStarted) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
                else -> break
            }
        }
    }

    private fun findEncoderForMimeType(mime: String): String? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) continue
            for (supportedType in codecInfo.supportedTypes) {
                if (supportedType.equals(mime, ignoreCase = true)) {
                    return codecInfo.name
                }
            }
        }
        return null
    }

    /**
     * 在原始 bitmap 上直接绘制（不复制），速度更快
     * 如果 bitmap 是 HARDWARE 配置则自动转换
     */
    private fun drawVideoResultsInPlace(
        bitmap: Bitmap,
        detections: List<OnnxFaceRecognition.FaceDetection>,
        names: List<String>
    ): Bitmap {
        val drawBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return bitmap
        } else {
            bitmap
        }

        val canvas = Canvas(drawBitmap)
        val imgWidth = drawBitmap.width
        val strokeW = max(3f, imgWidth / 300f)
        val textSz = max(24f, imgWidth / 40f)
        val dotR = max(2f, imgWidth / 500f)

        val boxPaint = Paint().apply {
            color = COLOR_GREEN
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = COLOR_GREEN
            textSize = textSz
            isAntiAlias = true
            isFakeBoldText = true
        }

        val bgPaint = Paint().apply {
            color = COLOR_BG
            style = Paint.Style.FILL
        }

        val landmarkPaint = Paint().apply {
            color = COLOR_YELLOW
            style = Paint.Style.FILL
        }

        val padding = 6f

        detections.forEachIndexed { index, detection ->
            val left = detection.rect.left.toFloat()
            val top = detection.rect.top.toFloat()
            val right = detection.rect.right.toFloat()
            val bottom = detection.rect.bottom.toFloat()

            canvas.drawRect(left, top, right, bottom, boxPaint)

            for ((lx, ly) in detection.landmarks) {
                canvas.drawCircle(lx, ly, dotR, landmarkPaint)
            }

            val name = names.getOrNull(index) ?: "Face"
            val confText = String.format("%.2f", detection.confidence)
            val fullText = "$name ($confText)"
            val textWidth = textPaint.measureText(fullText)

            if (top - textSz - padding * 2 >= 0) {
                val bgL = left
                val bgT = top - textSz - padding * 2
                val bgR = left + textWidth + padding * 2
                val bgB = top
                canvas.drawRect(bgL, bgT, bgR, bgB, bgPaint)
                canvas.drawText(fullText, bgL + padding, bgB - padding, textPaint)
            } else {
                val bgL = left
                val bgT = bottom
                val bgR = left + textWidth + padding * 2
                val bgB = bottom + textSz + padding * 2
                canvas.drawRect(bgL, bgT, bgR, bgB, bgPaint)
                canvas.drawText(fullText, bgL + padding, bgB - padding, textPaint)
            }
        }

        return drawBitmap
    }
}
