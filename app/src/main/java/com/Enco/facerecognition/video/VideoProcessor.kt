package com.Enco.facefound.video // 声明包名为视频处理模块

import android.content.Context // 导入Android上下文
import android.graphics.Bitmap // 导入位图类
import android.graphics.Canvas // 导入画布类，用于绘制
import android.graphics.Color // 导入颜色类
import android.graphics.Paint // 导入画笔类
import android.graphics.Rect // 导入矩形类
import android.media.MediaCodec // 导入媒体编解码器
import android.media.MediaCodecInfo // 导入编解码器信息
import android.media.MediaCodecList // 导入编解码器列表
import android.media.MediaFormat // 导入媒体格式
import android.media.MediaMetadataRetriever // 导入媒体元数据提取器
import android.media.MediaMuxer // 导入媒体复用器
import android.net.Uri // 导入URI类
import android.util.Log // 导入日志工具
import com.Enco.facefound.ml.OnnxFaceRecognition // 导入人脸识别引擎
import kotlinx.coroutines.Dispatchers // 导入协程调度器
import kotlinx.coroutines.channels.BufferOverflow // 导入缓冲区溢出策略
import kotlinx.coroutines.channels.Channel // 导入协程通道
import kotlinx.coroutines.flow.Flow // 导入Flow流
import kotlinx.coroutines.flow.channelFlow // 导入channelFlow构建器（支持协程内发射）
import kotlinx.coroutines.flow.flow // 导入flow构建器（extractFrames方法需要）
import kotlinx.coroutines.flow.flowOn // 导入flowOn操作符
import kotlinx.coroutines.isActive // 导入isActive协程状态检查
import kotlinx.coroutines.launch // 导入协程启动函数
import kotlinx.coroutines.withContext // 导入withContext协程切换
import java.io.File // 导入文件类
import java.nio.ByteBuffer // 导入字节缓冲区
import kotlin.coroutines.coroutineContext // 导入协程上下文
import kotlin.math.max // 导入max数学函数
import kotlin.math.min // 导入min数学函数

/**
 * 视频人脸识别处理器
 * 参考 video.py 的实现逻辑：
 * - 批量帧处理
 * - 绿色框 + 人名标注（video.py 绘制风格）
 * - 输出处理后的视频文件
 */
class VideoProcessor(private val faceRecognizer: OnnxFaceRecognition) { // 视频处理器类，接收人脸识别引擎实例

    companion object { // 伴生对象，存放常量
        private const val TAG = "VideoProcessor" // 日志标签
        private const val FRAME_SAMPLE_INTERVAL_US = 100_000L // 帧采样间隔（微秒），100ms = 10fps
        private const val TIMEOUT_US = 10_000L // 编解码器超时时间（微秒）
        private const val OUTPUT_MIME = "video/avc" // 输出视频MIME类型（H.264）
        private const val OUTPUT_BITRATE = 4_000_000 // 输出视频比特率（4Mbps）
        private const val OUTPUT_FRAME_RATE = 15 // 输出视频帧率
        private const val OUTPUT_I_FRAME_INTERVAL = 2 // I帧间隔（秒）
        private const val COLOR_GREEN = 0xFF00FF00.toInt() // 绿色，用于检测框
        private const val COLOR_YELLOW = 0xFFFFFF00.toInt() // 黄色，用于关键点
        private const val COLOR_BG = 0xA0000000.toInt() // 半透明黑色，用于文字背景
    }

    data class VideoInfo( // 视频信息数据类
        val durationMs: Long, // 视频时长（毫秒）
        val totalFrames: Int, // 总帧数
        val width: Int, // 视频宽度
        val height: Int, // 视频高度
        val rotation: Int, // 旋转角度
        val mimeType: String // MIME类型
    )

    data class ProcessedFrame( // 处理后的帧数据类
        val frameIndex: Int, // 帧序号
        val presentationTimeUs: Long, // 展示时间戳（微秒）
        val bitmap: Bitmap, // 绘制了检测结果的位图
        val detections: List<OnnxFaceRecognition.FaceDetection>, // 检测结果列表
        val names: List<String> // 识别出的人名列表
    )

    data class ProcessProgress( // 处理进度数据类
        val frameIndex: Int, // 当前帧序号
        val totalFrames: Int, // 总帧数
        val detections: List<OnnxFaceRecognition.FaceDetection>, // 当前帧检测结果
        val names: List<String> // 当前帧识别结果
    )

    fun getVideoInfo(context: Context, videoUri: Uri): VideoInfo { // 获取视频元信息
        val retriever = MediaMetadataRetriever() // 创建元数据提取器
        return try { // 异常保护
            retriever.setDataSource(context, videoUri) // 设置数据源
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) // 提取时长
            val durationMs = durationStr?.toLongOrNull() ?: 0L // 解析时长，默认0
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) // 提取宽度
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) // 提取高度
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) // 提取旋转角度
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/avc" // 提取MIME类型
            val width = widthStr?.toIntOrNull() ?: 1920 // 解析宽度，默认1920
            val height = heightStr?.toIntOrNull() ?: 1080 // 解析高度，默认1080
            val rotation = rotationStr?.toIntOrNull() ?: 0 // 解析旋转角度，默认0
            val totalFrames = if (durationMs > 0) ((durationMs / 1000f) * 30).toInt() else 0 // 估算总帧数（假设30fps）

            VideoInfo(durationMs, totalFrames, width, height, rotation, mimeType) // 返回视频信息
        } catch (e: Exception) { // 提取失败
            Log.e(TAG, "获取视频信息失败: ${e.message}") // 打印错误日志
            VideoInfo(0L, 0, 1920, 1080, 0, "video/avc") // 返回默认值
        } finally { // 清理资源
            try { retriever.release() } catch (_: Exception) {} // 释放提取器
        }
    }

    fun extractFramesWithRetriever( // 使用MediaMetadataRetriever提取视频帧
        context: Context, // 上下文
        videoUri: Uri // 视频URI
    ): Flow<Pair<Int, Bitmap>> = flow { // 返回帧序号与位图的Flow
        val retriever = MediaMetadataRetriever() // 创建元数据提取器
        try { // 异常保护
            retriever.setDataSource(context, videoUri) // 设置数据源
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) // 提取时长
            val durationMs = durationStr?.toLongOrNull() ?: 0L // 解析时长
            if (durationMs <= 0) return@flow // 时长无效则退出

            var timeUs = 0L // 当前时间位置（微秒）
            var frameIndex = 0 // 帧计数器
            val intervalUs = FRAME_SAMPLE_INTERVAL_US // 采样间隔

            while (timeUs <= durationMs * 1000) { // 循环直到超过视频时长
                if (!coroutineContext.isActive) break // 协程被取消则退出
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) // 获取最接近时间点的帧
                if (bitmap != null) { // 成功获取帧
                    emit(frameIndex to bitmap) // 发射帧数据
                }
                frameIndex++ // 递增帧计数
                timeUs += intervalUs // 推进时间位置
            }
        } catch (e: Exception) { // 提取异常
            Log.e(TAG, "提取帧失败: ${e.message}") // 打印错误日志
        } finally { // 清理资源
            try { retriever.release() } catch (_: Exception) {} // 释放提取器
        }
    }.flowOn(Dispatchers.IO) // 在IO线程执行

    fun processVideoFrames( // 处理视频帧并识别人脸（A2优化版：channelFlow流水线）
        context: Context, // 上下文
        videoUri: Uri, // 视频URI
        templates: Map<String, FloatArray>, // 人脸模板库
        threshold: Float, // 相似度阈值
        detectionThreshold: Float = 0.5f, // 检测置信度阈值
        onProgress: (ProcessProgress) -> Unit // 进度回调
    ): Flow<ProcessedFrame> = channelFlow { // 使用channelFlow支持协程内启动子协程
        val retriever = MediaMetadataRetriever() // 创建元数据提取器
        Log.d(TAG, "🎬 开始处理视频: $videoUri") // 记录开始处理视频的日志

        try { // 异常保护
            retriever.setDataSource(context, videoUri) // 设置数据源
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) // 提取时长（只提取一次）
            val durationMs = durationStr?.toLongOrNull() ?: 0L // 解析时长
            if (durationMs <= 0) { // 时长无效
                Log.e(TAG, "无法获取视频时长") // 打印错误日志
                return@channelFlow // 退出
            }
            Log.d(TAG, "📏 视频时长: ${durationMs}ms，使用OPTION_CLOSEST_SYNC加速帧提取") // 记录视频信息

            val intervalUs = FRAME_SAMPLE_INTERVAL_US // 采样间隔

            // === 创建帧通道，容量=3，实现预取缓冲区 ===
            val frameChannel = Channel<Triple<Bitmap?, Long, Int>>( // Triple<位图, 时间戳, 帧序号>
                capacity = 3, // 缓冲3帧，有效隐藏提取延迟
                onBufferOverflow = BufferOverflow.SUSPEND // 缓冲区满时挂起生产者
            )

            // === 生产者协程：提取帧并发送到通道 ===
            val producerJob = launch(Dispatchers.IO) { // 在IO线程运行生产者
                var timeUs = 0L // 当前时间位置（微秒）
                var frameIndex = 0 // 帧计数器

                try { // 异常保护
                    while (timeUs <= durationMs * 1000) { // 循环直到超过视频时长
                        if (!isActive) break // 协程被取消则退出

                        // 优化点：OPTION_CLOSEST_SYNC 定位到最近关键帧，比 OPTION_CLOSEST 快2x
                        val frameBitmap = retriever.getFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC // 关键帧定位，速度更快
                        )
                        frameChannel.send(Triple(frameBitmap, timeUs, frameIndex)) // 发送到通道

                        timeUs += intervalUs // 推进时间位置
                        frameIndex++ // 递增帧计数器
                    }
                    Log.d(TAG, "✅ 生产者完成：共提取 $frameIndex 帧") // 记录完成日志
                } catch (e: Exception) { // 捕获异常
                    Log.e(TAG, "❌ 生产者异常: ${e.message}") // 记录异常日志
                    frameChannel.close(e) // 关闭通道并传递异常
                    return@launch // 退出生产者协程
                } finally { // 最终清理
                    frameChannel.close() // 关闭通道，通知消费者没有更多数据
                }
            }

            // === 消费者：从通道取帧 → 推理 → 发射结果 ===
            var resultIndex = 0 // 结果帧计数器
            try { // 异常保护
                for ((frameBitmap, timeUs, _) in frameChannel) { // 从通道接收帧（挂起等待）
                    if (!coroutineContext.isActive) break // 协程被取消则退出

                    if (frameBitmap == null) { // 帧提取失败
                        resultIndex++ // 递增结果计数器
                        continue // 跳过此帧
                    }

                    // 执行人脸检测和识别（与生产者提取下一帧并行执行）
                    val detectionStartTime = System.currentTimeMillis() // 记录检测开始时间
                    val detections = faceRecognizer.detectFaces(frameBitmap, detectionThreshold) // 检测人脸

                    val results = faceRecognizer.recognizeFacesParallel( // 并行识别
                        frameBitmap, detections, templates, threshold
                    )
                    val names = results.map { it.name }.toMutableList() // 提取人名

                    val processingTime = System.currentTimeMillis() - detectionStartTime // 计算处理耗时
                    Log.v(TAG, "🖼️ 帧 $resultIndex 处理耗时: ${processingTime}ms, 检测到 ${detections.size} 张人脸")

                    val drawnBitmap = drawVideoResultsInPlace(frameBitmap, detections, names) // 绘制结果

                    onProgress(ProcessProgress(resultIndex, 0, detections, names)) // 回报进度

                    send( // 发射到Flow收集者
                        ProcessedFrame(
                            frameIndex = resultIndex,
                            presentationTimeUs = timeUs,
                            bitmap = drawnBitmap,
                            detections = detections,
                            names = names
                        )
                    )

                    resultIndex++ // 递增结果计数器
                }
                Log.d(TAG, "✅ 消费者完成：共处理 $resultIndex 帧") // 记录完成日志
            } catch (e: Exception) { // 捕获异常
                Log.e(TAG, "❌ 消费者异常: ${e.message}", e) // 记录异常日志
                throw e // 重新抛出异常
            }

            producerJob.join() // 等待生产者协程结束，确保资源正确释放

        } catch (e: Exception) { // 处理外层异常
            Log.e(TAG, "视频帧处理失败: ${e.message}", e) // 打印错误日志
            throw e // 重新抛出异常
        } finally { // 清理资源
            try { retriever.release() } catch (_: Exception) {} // 释放MediaMetadataRetriever资源
            Log.d(TAG, "🧹 视频处理器资源已释放") // 记录资源释放日志
        }
    }.flowOn(Dispatchers.Default) // 在Default调度器执行（推理为CPU密集型）

    suspend fun encodeToVideo( // 将处理后的帧编码为视频文件
        frames: List<ProcessedFrame>, // 处理后的帧列表
        outputFile: File, // 输出文件
        width: Int, // 输出宽度
        height: Int, // 输出高度
        onProgress: (Float) -> Unit // 进度回调（0~1）
    ): File = withContext(Dispatchers.IO) { // 在IO线程执行
        if (frames.isEmpty()) throw IllegalStateException("没有可编码的帧") // 帧列表为空则抛出异常

        val outputWidth = if (width > 0) width else frames.firstOrNull()?.bitmap?.width ?: 1920 // 确定输出宽度
        val outputHeight = if (height > 0) height else frames.firstOrNull()?.bitmap?.height ?: 1080 // 确定输出高度

        val alignedWidth = (outputWidth / 16) * 16 // 宽度对齐到16字节边界（编码器要求）
        val alignedHeight = (outputHeight / 16) * 16 // 高度对齐到16字节边界

        val muxer = MediaMuxer( // 创建媒体复用器
            outputFile.absolutePath, // 输出文件路径
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 // 输出格式为MP4
        )

        val format = MediaFormat.createVideoFormat(OUTPUT_MIME, alignedWidth, alignedHeight).apply { // 创建视频格式
            setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_BITRATE) // 设置比特率
            setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_FRAME_RATE) // 设置帧率
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_I_FRAME_INTERVAL) // 设置I帧间隔
            setInteger( // 设置颜色格式
                MediaFormat.KEY_COLOR_FORMAT, // 颜色格式键
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible // YUV420灵活格式
            )
        }

        val codecName = findEncoderForMimeType(OUTPUT_MIME) // 查找H.264编码器
            ?: throw IllegalStateException("找不到 H.264 编码器") // 未找到则抛出异常
        val encoder = MediaCodec.createByCodecName(codecName) // 创建编码器实例
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE) // 配置编码器为编码模式
        encoder.start() // 启动编码器

        val totalFrames = frames.size // 总帧数
        var muxerStarted = false // 标记复用器是否已启动
        var frameIdx = 0 // 已编码帧计数

        try { // 异常保护
            val tempBitmap = Bitmap.createBitmap(alignedWidth, alignedHeight, Bitmap.Config.ARGB_8888) // 创建临时位图用于缩放绘制
            val tempCanvas = Canvas(tempBitmap) // 创建画布
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) // 创建抗锯齿+双线性过滤画笔
            val pixelBuffer = IntArray(alignedWidth * alignedHeight) // 像素缓冲区
            val totalSize = alignedWidth * alignedHeight * 3 / 2 // YUV420数据总大小

            for (frame in frames) { // 遍历每一帧
                if (!coroutineContext.isActive) break // 协程被取消则退出

                val srcBitmap = frame.bitmap // 源位图
                if (!srcBitmap.isRecycled) { // 位图未被回收
                    tempCanvas.drawColor(Color.BLACK) // 清空为黑色背景
                    val scale = min( // 计算缩放比例（保持宽高比）
                        alignedWidth.toFloat() / srcBitmap.width, // 宽度缩放比
                        alignedHeight.toFloat() / srcBitmap.height // 高度缩放比
                    )
                    val scaledW = (srcBitmap.width * scale).toInt() // 缩放后宽度
                    val scaledH = (srcBitmap.height * scale).toInt() // 缩放后高度
                    val left = (alignedWidth - scaledW) / 2 // 水平居中偏移
                    val top = (alignedHeight - scaledH) / 2 // 垂直居中偏移
                    tempCanvas.drawBitmap(srcBitmap, null, Rect(left, top, left + scaledW, top + scaledH), paint) // 绘制缩放后的帧
                }

                val inputIndex = encoder.dequeueInputBuffer(TIMEOUT_US) // 获取可用输入缓冲区索引
                if (inputIndex >= 0) { // 有可用缓冲区
                    val buffer = encoder.getInputBuffer(inputIndex)!! // 获取输入缓冲区
                    buffer.clear() // 清空缓冲区

                    tempBitmap.getPixels(pixelBuffer, 0, alignedWidth, 0, 0, alignedWidth, alignedHeight) // 读取像素数据

                    if (buffer.capacity() >= totalSize) { // 缓冲区容量足够
                        convertARGBToNV12(pixelBuffer, buffer, alignedWidth, alignedHeight) // 转换ARGB为NV12格式

                        val pts = frame.presentationTimeUs.coerceAtLeast( // 计算展示时间戳
                            (frameIdx * 1_000_000L / OUTPUT_FRAME_RATE) // 基于帧率计算的时间戳
                        )

                        encoder.queueInputBuffer(inputIndex, 0, totalSize, pts, 0) // 将数据送入编码器
                        frameIdx++ // 递增已编码帧计数
                    } else { // 缓冲区不足
                        encoder.queueInputBuffer(inputIndex, 0, 0, 0, 0) // 提交空缓冲区
                    }
                }

                onProgress(frameIdx.toFloat() / totalFrames) // 回报编码进度
            }

            val eosIndex = encoder.dequeueInputBuffer(TIMEOUT_US) // 获取最后一个输入缓冲区
            if (eosIndex >= 0) { // 有可用缓冲区
                encoder.queueInputBuffer(eosIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM) // 发送流结束标记
            }

            drainEncoderFinal(encoder, muxer) { started, _ -> // 排出剩余编码数据
                muxerStarted = started // 更新复用器状态
            }

            tempBitmap.recycle() // 释放临时位图
        } catch (e: Exception) { // 编码异常
            Log.e(TAG, "编码视频失败: ${e.message}", e) // 打印错误日志
            throw e // 重新抛出异常
        } finally { // 清理资源
            try { encoder.stop() } catch (_: Exception) {} // 停止编码器
            try { encoder.release() } catch (_: Exception) {} // 释放编码器
            try { // 清理复用器
                if (muxerStarted) muxer.stop() // 停止复用器（仅在已启动时）
                muxer.release() // 释放复用器
            } catch (_: Exception) {} // 忽略清理异常
        }

        onProgress(1f) // 回报100%进度
        outputFile // 返回输出文件
    }

    /**
     * 将 ARGB_8888 像素转换为 NV12 (YUV420 semi-planar) 格式
     * NV12 布局: [Y平面 (W*H)] [UV交错 (W*H/2)]
     */
    private fun convertARGBToNV12( // 将ARGB像素数组转换为NV12格式
        pixels: IntArray, // ARGB像素数组
        outputBuffer: ByteBuffer, // 输出字节缓冲区
        width: Int, // 图像宽度
        height: Int // 图像高度
    ) {
        val yPlaneSize = width * height // Y平面大小

        var yIdx = 0 // Y平面写入索引
        var uvIdx = yPlaneSize // UV平面写入索引（紧跟Y平面之后）

        var py = 0 // 像素行坐标
        while (py < height) { // 遍历每一行
            val rowOffset = py * width // 当前行在像素数组中的偏移
            var px = 0 // 像素列坐标
            while (px < width) { // 遍历每一列
                val pixel = pixels[rowOffset + px] // 读取当前像素（ARGB格式）
                val r = (pixel shr 16) and 0xFF // 提取红色通道
                val g = (pixel shr 8) and 0xFF // 提取绿色通道
                val b = pixel and 0xFF // 提取蓝色通道

                val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16 // BT.601标准RGB转Y公式
                outputBuffer.put(yIdx, yVal.coerceIn(0, 255).toByte()) // 写入Y值，钳位到[0,255]
                yIdx++ // 递增Y索引

                if ((py and 1) == 0 && (px and 1) == 0) { // 每2x2像素块取一个UV采样
                    val uVal = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128 // BT.601标准RGB转U公式
                    val vVal = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128 // BT.601标准RGB转V公式
                    outputBuffer.put(uvIdx, uVal.coerceIn(0, 255).toByte()) // 写入U值
                    outputBuffer.put(uvIdx + 1, vVal.coerceIn(0, 255).toByte()) // 写入V值
                    uvIdx += 2 // 递增UV索引（每次写2字节）
                }
                px++ // 递增列坐标
            }
            py++ // 递增行坐标
        }
    }

    /**
     * 从编码器排出已编码数据并写入 muxer
     */
    private fun drainEncoderFinal( // 排出编码器中所有剩余数据
        encoder: MediaCodec, // 编码器实例
        muxer: MediaMuxer, // 复用器实例
        onFormatChanged: (Boolean, Int) -> Unit // 格式变更回调
    ) {
        val bufferInfo = MediaCodec.BufferInfo() // 创建缓冲区信息对象
        var muxerStarted = false // 复用器启动标记
        var trackIndex = -1 // 轨道索引

        while (true) { // 循环排出所有输出
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US) // 获取输出缓冲区索引
            when { // 根据返回值处理
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { // 输出格式变更（编码器准备好）
                    if (!muxerStarted) { // 复用器尚未启动
                        trackIndex = muxer.addTrack(encoder.outputFormat) // 添加编码器输出轨道
                        muxer.start() // 启动复用器
                        muxerStarted = true // 标记已启动
                        onFormatChanged(true, trackIndex) // 回调通知
                    }
                }
                outputIndex >= 0 -> { // 有可用输出缓冲区
                    val outputBuffer = encoder.getOutputBuffer(outputIndex)!! // 获取输出缓冲区
                    if (bufferInfo.size > 0 && muxerStarted) { // 有数据且复用器已启动
                        outputBuffer.position(bufferInfo.offset) // 设置读取位置
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size) // 设置读取上限
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo) // 写入复用器
                    }
                    encoder.releaseOutputBuffer(outputIndex, false) // 释放输出缓冲区
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break // 收到流结束标记则退出
                }
                else -> break // 其他情况（超时等）退出
            }
        }
    }

    private fun findEncoderForMimeType(mime: String): String? { // 根据MIME类型查找编码器名称
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS) // 获取常规编解码器列表
        for (codecInfo in codecList.codecInfos) { // 遍历所有编解码器
            if (!codecInfo.isEncoder) continue // 跳过解码器
            for (supportedType in codecInfo.supportedTypes) { // 遍历支持的类型
                if (supportedType.equals(mime, ignoreCase = true)) { // 类型匹配
                    return codecInfo.name // 返回编码器名称
                }
            }
        }
        return null // 未找到返回null
    }

    /**
     * 在原始 bitmap 上直接绘制（不复制），速度更快
     * 如果 bitmap 是 HARDWARE 配置则自动转换
     */
    private fun drawVideoResultsInPlace( // 在位图上直接绘制检测结果
        bitmap: Bitmap, // 源位图
        detections: List<OnnxFaceRecognition.FaceDetection>, // 检测结果
        names: List<String> // 人名列表
    ): Bitmap {
        val drawBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) { // 如果是硬件位图
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return bitmap // 转换为软件位图，失败则返回原图
        } else { // 已经是软件位图
            bitmap // 直接使用
        }

        val canvas = Canvas(drawBitmap) // 创建画布
        val imgWidth = drawBitmap.width // 图像宽度
        val strokeW = max(3f, imgWidth / 300f) // 根据图像宽度自适应线条粗细
        val textSz = max(24f, imgWidth / 40f) // 根据图像宽度自适应文字大小
        val dotR = max(2f, imgWidth / 500f) // 根据图像宽度自适应关键点半径

        val boxPaint = Paint().apply { // 创建检测框画笔
            color = COLOR_GREEN // 绿色
            style = Paint.Style.STROKE // 描边样式
            strokeWidth = strokeW // 线条粗细
            isAntiAlias = true // 开启抗锯齿
        }

        val textPaint = Paint().apply { // 创建文字画笔
            color = COLOR_GREEN // 绿色
            textSize = textSz // 文字大小
            isAntiAlias = true // 开启抗锯齿
            isFakeBoldText = true // 伪粗体
        }

        val bgPaint = Paint().apply { // 创建背景画笔
            color = COLOR_BG // 半透明黑色
            style = Paint.Style.FILL // 填充样式
        }

        val landmarkPaint = Paint().apply { // 创建关键点画笔
            color = COLOR_YELLOW // 黄色
            style = Paint.Style.FILL // 填充样式
        }

        val padding = 6f // 文字内边距

        detections.forEachIndexed { index, detection -> // 遍历每个检测结果
            val left = detection.rect.left.toFloat() // 左边界
            val top = detection.rect.top.toFloat() // 上边界
            val right = detection.rect.right.toFloat() // 右边界
            val bottom = detection.rect.bottom.toFloat() // 下边界

            canvas.drawRect(left, top, right, bottom, boxPaint) // 绘制检测框

            for ((lx, ly) in detection.landmarks) { // 遍历关键点
                canvas.drawCircle(lx, ly, dotR, landmarkPaint) // 绘制关键点圆点
            }

            val name = names.getOrNull(index) ?: "Face" // 获取人名，默认"Face"
            val confText = String.format("%.2f", detection.confidence) // 格式化置信度
            val fullText = "$name ($confText)" // 拼接标签文本
            val textWidth = textPaint.measureText(fullText) // 测量文本宽度

            if (top - textSz - padding * 2 >= 0) { // 检测框上方有足够空间
                val bgL = left // 背景左边界
                val bgT = top - textSz - padding * 2 // 背景上边界
                val bgR = left + textWidth + padding * 2 // 背景右边界
                val bgB = top // 背景下边界
                canvas.drawRect(bgL, bgT, bgR, bgB, bgPaint) // 绘制文字背景
                canvas.drawText(fullText, bgL + padding, bgB - padding, textPaint) // 绘制文字
            } else { // 上方空间不足，绘制在下方
                val bgL = left // 背景左边界
                val bgT = bottom // 背景上边界
                val bgR = left + textWidth + padding * 2 // 背景右边界
                val bgB = bottom + textSz + padding * 2 // 背景下边界
                canvas.drawRect(bgL, bgT, bgR, bgB, bgPaint) // 绘制文字背景
                canvas.drawText(fullText, bgL + padding, bgB - padding, textPaint) // 绘制文字
            }
        }

        return drawBitmap // 返回绘制后的位图
    }
}
