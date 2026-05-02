package com.Enco.facefound.ml

import ai.onnxruntime.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ONNX Runtime 人脸识别引擎
 * 针对骁龙 8 系列旗舰芯片深度优化
 */
class OnnxFaceRecognition(context: Context) {

    companion object {
        private const val TAG = "OnnxFaceRecognition"

        const val DETECTION_MODEL = "det_10g.onnx"
        const val RECOGNITION_MODEL = "w600k_r50.onnx"

        const val DET_INPUT_SIZE = 640
        const val REC_INPUT_SIZE = 112
        const val EMBEDDING_DIM = 512

        const val DET_CONF_THRESHOLD = 0.5f
        const val DET_IOU_THRESHOLD = 0.45f

        // 模型文件预期大小（用于校验）
        const val DET_MODEL_SIZE_MB = 16L
        const val REC_MODEL_SIZE_MB = 166L

        // 骁龙 8 系列最优线程数
        const val SD8_OPTIMAL_THREADS = 6
    }

    private val appContext = context.applicationContext
    private var detSession: OrtSession? = null
    private var recSession: OrtSession? = null
    private var env: OrtEnvironment? = null
    private var detInputName: String = "input"
    private var recInputName: String = "input"

    private data class OutputShapeInfo(val name: String, val lastDim: Int, val anchorCount: Int)
    private var detOutputShapes: List<OutputShapeInfo> = emptyList()

    // 缓冲区大小常量
    private val detPixelBufferSize = DET_INPUT_SIZE * DET_INPUT_SIZE
    private val detFloatBufferSize = 3 * DET_INPUT_SIZE * DET_INPUT_SIZE
    private val recPixelBufferSize = REC_INPUT_SIZE * REC_INPUT_SIZE
    private val recFloatBufferSize = 3 * REC_INPUT_SIZE * REC_INPUT_SIZE

    // 设备信息
    private val isSnapdragon8 = isSnapdragon8Series()
    private val optimalThreads = if (isSnapdragon8) SD8_OPTIMAL_THREADS else max(1, min(4, Runtime.getRuntime().availableProcessors() / 2))

    var isLoaded = false
        private set

    init {
        try {
            initModels()
        } catch (e: Exception) {
            Log.e(TAG, "模型初始化失败: ${e.message}", e)
        }
    }

    private fun initModels() {
        Log.i(TAG, "========================================")
        Log.i(TAG, "开始初始化 ONNX Runtime")
        Log.i(TAG, "设备: ${if (isSnapdragon8) "骁龙 8 系列" else "其他"}")
        Log.i(TAG, "线程数: $optimalThreads")
        Log.i(TAG, "========================================")
        val startTime = System.currentTimeMillis()

        // 检查 assets 目录中的文件
        try {
            val assets = appContext.assets.list("")
            if (assets != null) {
                Log.d(TAG, "Assets 目录文件列表:")
                assets.forEach { fileName ->
                    Log.d(TAG, "  - $fileName")
                }

                if (!assets.contains(DETECTION_MODEL)) {
                    Log.e(TAG, "⚠️ 检测模型缺失: $DETECTION_MODEL")
                }
                if (!assets.contains(RECOGNITION_MODEL)) {
                    Log.e(TAG, "⚠️ 识别模型缺失: $RECOGNITION_MODEL")
                }
            } else {
                Log.w(TAG, "⚠️ Assets 目录为空或无法读取")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 检查 Assets 目录失败: ${e.message}")
        }

        env = OrtEnvironment.getEnvironment()
        Log.i(TAG, "ONNX Runtime 环境初始化成功")

        val detSessionOptions = OrtSession.SessionOptions().apply {
            setMemoryPatternOptimization(true)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setIntraOpNumThreads(optimalThreads)
            setInterOpNumThreads(optimalThreads)
        }

        val recSessionOptions = OrtSession.SessionOptions().apply {
            setMemoryPatternOptimization(true)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setIntraOpNumThreads(optimalThreads)
            setInterOpNumThreads(optimalThreads)
        }

        try {
            // 加载检测模型
            Log.i(TAG, "----------------------------------------")
            Log.i(TAG, "加载检测模型...")
            val detModelFile = copyModelFromAssets(DETECTION_MODEL, DET_MODEL_SIZE_MB)
            if (detModelFile != null && validateModelFile(detModelFile, DET_MODEL_SIZE_MB)) {
                try {
                    detSession = env?.createSession(detModelFile.absolutePath, detSessionOptions)
                    detInputName = detSession?.inputNames?.firstOrNull() ?: "input"
                    Log.i(TAG, "✅ 检测模型加载成功")
                    val inputNames = detSession?.inputNames?.joinToString(", ")
                    Log.d(TAG, "检测模型输入: $inputNames (使用: $detInputName)")
                    val outputNames = detSession?.outputNames?.joinToString(", ")
                    Log.d(TAG, "检测模型输出: $outputNames")
                    try {
                        detSession?.inputInfo?.forEach { (name, nodeInfo) ->
                            Log.i(TAG, "  输入 '$name': ${nodeInfo.info}")
                        }
                        detSession?.outputInfo?.forEach { (name, nodeInfo) ->
                            Log.i(TAG, "  输出 '$name': ${nodeInfo.info}")
                        }
                        detOutputShapes = detSession?.outputInfo?.map { (name, nodeInfo) ->
                            val info = nodeInfo.info
                            if (info is TensorInfo) {
                                val shape = info.shape
                                val lastDim = if (shape.isNotEmpty()) shape.last().toInt() else 0
                                val anchorCount = if (shape.size >= 2) shape.first().toInt() else 0
                                OutputShapeInfo(name, lastDim, anchorCount)
                            } else {
                                OutputShapeInfo(name, 0, 0)
                            }
                        } ?: emptyList()
                        Log.i(TAG, "  缓存输出形状: ${detOutputShapes.map { "${it.name}[${it.anchorCount},${it.lastDim}]" }}")
                    } catch (e: Exception) {
                        Log.w(TAG, "读取模型I/O信息失败: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 创建检测模型会话失败: ${e.javaClass.simpleName} - ${e.message}")
                    e.printStackTrace()
                }
            } else {
                Log.e(TAG, "❌ 检测模型加载失败")
            }
        } finally {
            detSessionOptions.close()
        }

        try {
            // 加载识别模型
            Log.i(TAG, "----------------------------------------")
            Log.i(TAG, "加载识别模型...")
            val recModelFile = copyModelFromAssets(RECOGNITION_MODEL, REC_MODEL_SIZE_MB)
            if (recModelFile != null && validateModelFile(recModelFile, REC_MODEL_SIZE_MB)) {
                try {
                    recSession = env?.createSession(recModelFile.absolutePath, recSessionOptions)
                    recInputName = recSession?.inputNames?.firstOrNull() ?: "input"
                    Log.i(TAG, "✅ 识别模型加载成功")
                    val inputNames = recSession?.inputNames?.joinToString(", ")
                    Log.d(TAG, "识别模型输入: $inputNames (使用: $recInputName)")
                    val outputNames = recSession?.outputNames?.joinToString(", ")
                    Log.d(TAG, "识别模型输出: $outputNames")
                    try {
                        recSession?.inputInfo?.forEach { (name, nodeInfo) ->
                            Log.i(TAG, "  输入 '$name': ${nodeInfo.info}")
                        }
                        recSession?.outputInfo?.forEach { (name, nodeInfo) ->
                            Log.i(TAG, "  输出 '$name': ${nodeInfo.info}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "读取模型I/O信息失败: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 创建识别模型会话失败: ${e.javaClass.simpleName} - ${e.message}")
                    e.printStackTrace()
                }
            } else {
                Log.e(TAG, "❌ 识别模型加载失败")
            }
        } finally {
            recSessionOptions.close()
        }

        isLoaded = detSession != null && recSession != null
        val elapsed = System.currentTimeMillis() - startTime

        Log.i(TAG, "========================================")
        if (isLoaded) {
            Log.i(TAG, "✅ 所有模型加载完成，耗时 ${elapsed}ms")
        } else {
            Log.e(TAG, "❌ 部分模型加载失败")
        }
        Log.i(TAG, "========================================")
    }

    /**
     * 检测是否为骁龙 8 系列
     */
    private fun isSnapdragon8Series(): Boolean {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val device = Build.DEVICE.lowercase()
        val check = hardware + board + device
        return check.contains("sm8") || check.contains("sdm8") || check.contains("taro") ||
               check.contains("kalama") || check.contains("pineapple") ||
               (check.contains("snapdragon") && check.contains("gen"))
    }

    private fun validateModelFile(file: File, expectedSizeMb: Long): Boolean {
        val expectedBytes = expectedSizeMb * 1024 * 1024
        val isValid = file.length() >= expectedBytes * 0.8
        if (!isValid) {
            val sizeMb = file.length() / (1024 * 1024)
            Log.w(TAG, "模型文件大小异常: ${file.name} = ${sizeMb}MB (预期 >= ${expectedSizeMb * 0.8}MB)")
        }
        return isValid
    }

    /**
     * 从 assets 复制模型到缓存目录
     */
    private fun copyModelFromAssets(modelName: String, expectedSizeMb: Long = 0L): File? {
        return try {
            val outFile = File(appContext.cacheDir, modelName)

            if (outFile.exists() && outFile.length() > expectedSizeMb * 1024 * 1024 * 0.8) {
                Log.d(TAG, "模型已缓存: ${outFile.absolutePath}")
                return outFile
            }

            appContext.assets.open(modelName).use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            Log.i(TAG, "模型复制完成: ${outFile.absolutePath} (${outFile.length() / 1024 / 1024}MB)")
            outFile

        } catch (e: Exception) {
            Log.e(TAG, "复制模型失败 $modelName: ${e.message}")
            null
        }
    }

    /**
     * 检测人脸 - 自动识别单/多输出格式
     */
    suspend fun detectFaces(bitmap: Bitmap): List<FaceDetection> = withContext(Dispatchers.Default) {
        if (detSession == null || env == null) {
            Log.e(TAG, "检测模型未加载")
            return@withContext emptyList()
        }

        var inputTensor: OnnxTensor? = null
        var outputs: OrtSession.Result? = null

        try {
            val startTime = System.currentTimeMillis()

            inputTensor = preprocessDetection(bitmap)
            outputs = detSession?.run(mapOf(detInputName to inputTensor))

            val outputCount = outputs?.size() ?: 0
            Log.i(TAG, "模型输出张量数: $outputCount")

            if (outputCount > 0) {
                for (i in 0 until min(outputCount, 20)) {
                    try {
                        val rawVal = outputs!!.get(i).value
                        val flatArr = extractFlatFloatArray(rawVal)
                        val n = minOf(10, flatArr.size)
                        val sample = flatArr.take(n).joinToString(", ") { "%.6f".format(it) }
                        val minVal = if (flatArr.isNotEmpty()) flatArr.minOrNull() else 0f
                        val maxVal = if (flatArr.isNotEmpty()) flatArr.maxOrNull() else 0f
                        Log.i(TAG, "  输出[$i]: flatSize=${flatArr.size}, range=[${"%.4f".format(minVal)}, ${"%.4f".format(maxVal)}], sample=[$sample]")
                    } catch (e: Exception) {
                        Log.w(TAG, "  输出[$i]: 读取失败 - ${e.message}")
                    }
                }
            }

            var detections = if (outputCount >= 2) {
                parseMultiStrideOutputs(outputs!!, bitmap.width, bitmap.height)
            } else {
                parseDetectionOutputs(outputs, bitmap.width, bitmap.height)
            }

            if (detections.isEmpty() && outputCount >= 1) {
                Log.w(TAG, "主解析策略返回0，尝试回退单输出解析...")
                detections = parseDetectionOutputs(outputs, bitmap.width, bitmap.height)
            }

            if (detections.isEmpty() && outputCount > 0) {
                Log.w(TAG, "所有解析策略返回0，执行诊断扫描...")
                diagnosticScanOutputs(outputs!!, outputCount)
            }

            val nmsDetections = applyNMS(detections, DET_IOU_THRESHOLD)

            val elapsed = System.currentTimeMillis() - startTime
            Log.i(TAG, "检测完成: ${nmsDetections.size} 张人脸, 耗时 ${elapsed}ms")

            nmsDetections

        } catch (e: Exception) {
            Log.e(TAG, "检测失败: ${e.message}", e)
            emptyList()
        } finally {
            try { inputTensor?.close() } catch (_: Exception) {}
            try { outputs?.close() } catch (_: Exception) {}
        }
    }

    /**
     * 人脸对齐 - 使用5点关键点最小二乘相似变换
     * ArcFace 标准对齐目标点 (112x112)
     */
    fun alignFace(sourceBitmap: Bitmap, detection: FaceDetection): Bitmap {
        if (detection.landmarks.size < 2) {
            return cropFaceWithMargin(sourceBitmap, detection.rect)
        }

        val dstPoints = floatArrayOf(
            38.2946f, 51.6963f,
            73.5318f, 51.5014f,
            56.0252f, 71.7366f,
            41.5493f, 92.3655f,
            70.7299f, 92.2041f
        )

        val numPoints = minOf(detection.landmarks.size, 5)
        val srcPoints = FloatArray(numPoints * 2)
        for (i in 0 until numPoints) {
            srcPoints[i * 2] = detection.landmarks[i].first
            srcPoints[i * 2 + 1] = detection.landmarks[i].second
        }

        val matrix = estimateSimilarityTransform(srcPoints, dstPoints, numPoints)

        val alignedBitmap = Bitmap.createBitmap(REC_INPUT_SIZE, REC_INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(alignedBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        canvas.drawBitmap(sourceBitmap, matrix, paint)

        return alignedBitmap
    }

    private fun estimateSimilarityTransform(
        src: FloatArray, dst: FloatArray, numPoints: Int
    ): Matrix {
        val n = numPoints
        val a11 = FloatArray(n) { i -> src[i * 2] }
        val a12 = FloatArray(n) { i -> -src[i * 2 + 1] }
        val a13 = FloatArray(n) { 1f }
        val a14 = FloatArray(n) { 0f }
        val a21 = FloatArray(n) { i -> src[i * 2 + 1] }
        val a22 = FloatArray(n) { i -> src[i * 2] }
        val a23 = FloatArray(n) { 0f }
        val a24 = FloatArray(n) { 1f }
        val bx = FloatArray(n) { i -> dst[i * 2] }
        val by = FloatArray(n) { i -> dst[i * 2 + 1] }

        val ata = Array(4) { FloatArray(4) }
        val atb = FloatArray(4)

        for (r in 0 until 4) {
            for (c in 0 until 4) {
                var sum = 0f
                for (i in 0 until n) {
                    val ar = when (r) { 0 -> a11[i]; 1 -> a12[i]; 2 -> a13[i]; else -> a14[i] }
                    val ac = when (c) { 0 -> a11[i]; 1 -> a12[i]; 2 -> a13[i]; else -> a14[i] }
                    sum += ar * ac
                    val ar2 = when (r) { 0 -> a21[i]; 1 -> a22[i]; 2 -> a23[i]; else -> a24[i] }
                    val ac2 = when (c) { 0 -> a21[i]; 1 -> a22[i]; 2 -> a23[i]; else -> a24[i] }
                    sum += ar2 * ac2
                }
                ata[r][c] = sum
            }
            var sumB = 0f
            for (i in 0 until n) {
                val ar = when (r) { 0 -> a11[i]; 1 -> a12[i]; 2 -> a13[i]; else -> a14[i] }
                sumB += ar * bx[i]
                val ar2 = when (r) { 0 -> a21[i]; 1 -> a22[i]; 2 -> a23[i]; else -> a24[i] }
                sumB += ar2 * by[i]
            }
            atb[r] = sumB
        }

        val params = solve4x4(ata, atb) ?: return Matrix()

        val a = params[0]
        val b = params[1]
        val tx = params[2]
        val ty = params[3]

        val values = floatArrayOf(a, -b, tx, b, a, ty, 0f, 0f, 1f)
        return Matrix().apply { setValues(values) }
    }

    private fun solve4x4(A: Array<FloatArray>, b: FloatArray): FloatArray? {
        val n = 4
        val aug = Array(n) { FloatArray(n + 1) }
        for (i in 0 until n) {
            for (j in 0 until n) aug[i][j] = A[i][j]
            aug[i][n] = b[i]
        }

        for (col in 0 until n) {
            var maxRow = col
            var maxVal = kotlin.math.abs(aug[col][col])
            for (row in col + 1 until n) {
                val v = kotlin.math.abs(aug[row][col])
                if (v > maxVal) { maxVal = v; maxRow = row }
            }
            if (maxVal < 1e-10f) return null

            val tmp = aug[col]; aug[col] = aug[maxRow]; aug[maxRow] = tmp

            val pivot = aug[col][col]
            for (j in col..n) aug[col][j] /= pivot

            for (row in 0 until n) {
                if (row == col) continue
                val factor = aug[row][col]
                for (j in col..n) aug[row][j] -= factor * aug[col][j]
            }
        }

        return FloatArray(n) { i -> aug[i][n] }
    }

    /**
     * 带边距的人脸裁剪（无关键点时的回退方案）
     */
    private fun cropFaceWithMargin(sourceBitmap: Bitmap, rect: Rect): Bitmap {
        val w = rect.right - rect.left
        val h = rect.bottom - rect.top
        val centerX = rect.left + w / 2f
        val centerY = rect.top + h / 2f
        val size = maxOf(w, h) * 1.2f

        val left = (centerX - size / 2).toInt().coerceIn(0, sourceBitmap.width - 1)
        val top = (centerY - size / 2).toInt().coerceIn(0, sourceBitmap.height - 1)
        val right = (centerX + size / 2).toInt().coerceIn(left + 1, sourceBitmap.width)
        val bottom = (centerY + size / 2).toInt().coerceIn(top + 1, sourceBitmap.height)

        val cropW = right - left
        val cropH = bottom - top
        if (cropW <= 0 || cropH <= 0) {
            return Bitmap.createScaledBitmap(sourceBitmap, REC_INPUT_SIZE, REC_INPUT_SIZE, true)
        }

        val cropped = Bitmap.createBitmap(sourceBitmap, left, top, cropW, cropH)
        val scaled = Bitmap.createScaledBitmap(cropped, REC_INPUT_SIZE, REC_INPUT_SIZE, true)
        cropped.recycle()
        return scaled
    }

    /**
     * 提取人脸特征 - 高性能版本
     */
    suspend fun extractEmbedding(faceBitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        if (recSession == null || env == null) {
            Log.e(TAG, "识别模型未加载")
            return@withContext FloatArray(EMBEDDING_DIM)
        }

        var inputTensor: OnnxTensor? = null
        var outputs: OrtSession.Result? = null

        try {
            val startTime = System.currentTimeMillis()

            inputTensor = preprocessRecognition(faceBitmap)
            outputs = recSession?.run(mapOf(recInputName to inputTensor))

            val rawEmbedding = when (val outputValue = outputs?.get(0)?.value) {
                is Array<*> -> (outputValue.getOrNull(0) as? FloatArray)
                    ?: FloatArray(EMBEDDING_DIM)
                is FloatArray -> outputValue
                else -> FloatArray(EMBEDDING_DIM)
            }

            val normalizedEmbedding = l2Normalize(rawEmbedding)

            val elapsed = System.currentTimeMillis() - startTime
            val norm = sqrt(rawEmbedding.fold(0f) { acc, v -> acc + v * v })
            val sample = normalizedEmbedding.take(5).joinToString(", ") { "%.4f".format(it) }
            Log.d(TAG, "特征提取完成, 耗时 ${elapsed}ms, dim=${rawEmbedding.size}, norm=${"%.4f".format(norm)}, sample=[$sample]")

            normalizedEmbedding

        } catch (e: Exception) {
            Log.e(TAG, "特征提取失败: ${e.message}", e)
            FloatArray(EMBEDDING_DIM)
        } finally {
            try { inputTensor?.close() } catch (_: Exception) {}
            try { outputs?.close() } catch (_: Exception) {}
        }
    }

    /**
     * 识别人脸 - 批量比对优化
     */
    suspend fun recognizeFace(
        faceBitmap: Bitmap,
        templates: Map<String, FloatArray>,
        threshold: Float = 0.45f
    ): RecognitionResult = withContext(Dispatchers.Default) {

        if (templates.isEmpty()) {
            Log.w(TAG, "识别: 模板库为空")
            return@withContext RecognitionResult("UNKNOWN", 0f)
        }

        val validTemplates = templates.filter { it.value.size == EMBEDDING_DIM }
        if (validTemplates.size < templates.size) {
            Log.w(TAG, "识别: ${templates.size - validTemplates.size} 个模板维度不匹配被跳过 (需要${EMBEDDING_DIM}维)")
        }

        val embedding = extractEmbedding(faceBitmap)

        var bestName = "UNKNOWN"
        var bestScore = 0f
        var secondBestScore = 0f

        for ((name, templateEmbedding) in validTemplates) {
            val similarity = cosineSimilarity(embedding, templateEmbedding)
            if (similarity > bestScore) {
                secondBestScore = bestScore
                bestScore = similarity
                bestName = name
            } else if (similarity > secondBestScore) {
                secondBestScore = similarity
            }
        }

        Log.d(TAG, "识别结果: best=$bestName score=${"%.4f".format(bestScore)} " +
            "second=${"%.4f".format(secondBestScore)} threshold=$threshold " +
            "validTemplates=${validTemplates.size}")

        if (bestScore >= threshold) {
            RecognitionResult(bestName, bestScore)
        } else {
            RecognitionResult("UNKNOWN", bestScore)
        }
    }

    private fun ensureSoftware(bitmap: Bitmap): Bitmap {
        if (bitmap.config == Bitmap.Config.HARDWARE) {
            val sw = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            if (sw != null) return sw
            Log.w(TAG, "HARDWARE→ARGB_8888 失败，尝试 createScaledBitmap")
            val scaled = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
            if (scaled.config != Bitmap.Config.HARDWARE) return scaled
        }
        return bitmap
    }

    /**
     * 预处理检测输入 - InsightFace SCRFD 标准预处理
     * mean=[127.5, 127.5, 127.5], std=[128.0, 128.0, 128.0], BGR 格式
     */
    private fun preprocessDetection(bitmap: Bitmap): OnnxTensor {
        val safeBitmap = ensureSoftware(bitmap)
        Log.d(TAG, "原始图片: ${bitmap.width}x${bitmap.height} config=${bitmap.config} → safe=${safeBitmap.config}")
        val scaledBitmap = Bitmap.createScaledBitmap(safeBitmap, DET_INPUT_SIZE, DET_INPUT_SIZE, true)
        if (safeBitmap !== bitmap) safeBitmap.recycle()
        
        val pixelBuffer = IntArray(detPixelBufferSize)
        val floatBuffer = FloatArray(detFloatBufferSize)

        try {
            scaledBitmap.getPixels(pixelBuffer, 0, DET_INPUT_SIZE, 0, 0, DET_INPUT_SIZE, DET_INPUT_SIZE)

            val size = DET_INPUT_SIZE * DET_INPUT_SIZE
            for (i in 0 until size) {
                val pixel = pixelBuffer[i]
                floatBuffer[i] = ((pixel and 0xFF) - 127.5f) / 128.0f
                floatBuffer[i + size] = (((pixel shr 8) and 0xFF) - 127.5f) / 128.0f
                floatBuffer[i + 2 * size] = (((pixel shr 16) and 0xFF) - 127.5f) / 128.0f
            }

            val sample = pixelBuffer.take(5).joinToString(", ") { "0x%08X".format(it) }
            Log.d(TAG, "前5像素: $sample")
            Log.d(TAG, "float范围: [${"%.4f".format(floatBuffer.minOrNull())}, ${"%.4f".format(floatBuffer.maxOrNull())}]")

            val shape = longArrayOf(1, 3, DET_INPUT_SIZE.toLong(), DET_INPUT_SIZE.toLong())
            return OnnxTensor.createTensor(env, FloatBuffer.wrap(floatBuffer), shape)
        } finally {
            scaledBitmap.recycle()
        }
    }

    /**
     * 预处理识别输入 - InsightFace ArcFace 标准预处理
     * mean=[127.5, 127.5, 127.5], std=[128.0, 128.0, 128.0], BGR 格式
     */
    private fun preprocessRecognition(bitmap: Bitmap): OnnxTensor {
        val safeBitmap = ensureSoftware(bitmap)
        val scaledBitmap = Bitmap.createScaledBitmap(safeBitmap, REC_INPUT_SIZE, REC_INPUT_SIZE, true)
        if (safeBitmap !== bitmap) safeBitmap.recycle()
        
        val pixelBuffer = IntArray(recPixelBufferSize)
        val floatBuffer = FloatArray(recFloatBufferSize)

        try {
            scaledBitmap.getPixels(pixelBuffer, 0, REC_INPUT_SIZE, 0, 0, REC_INPUT_SIZE, REC_INPUT_SIZE)

            val size = REC_INPUT_SIZE * REC_INPUT_SIZE
            for (i in 0 until size) {
                val pixel = pixelBuffer[i]
                floatBuffer[i] = ((pixel and 0xFF) - 127.5f) / 128.0f
                floatBuffer[i + size] = (((pixel shr 8) and 0xFF) - 127.5f) / 128.0f
                floatBuffer[i + 2 * size] = (((pixel shr 16) and 0xFF) - 127.5f) / 128.0f
            }

            val shape = longArrayOf(1, 3, REC_INPUT_SIZE.toLong(), REC_INPUT_SIZE.toLong())
            return OnnxTensor.createTensor(env, FloatBuffer.wrap(floatBuffer), shape)
        } finally {
            scaledBitmap.recycle()
        }
    }

    /**
     * 解析检测输出 - 支持 InsightFace 的 det_10g 输出格式
     * 格式: [1, N, 15] = [x1, y1, x2, y2, score, kp0_x, kp0_y, ..., kp4_y]
     */
    private fun parseDetectionOutputs(
        outputs: OrtSession.Result?,
        imgWidth: Int,
        imgHeight: Int
    ): List<FaceDetection> {
        val detections = mutableListOf<FaceDetection>()
        val scaleX = imgWidth.toFloat() / DET_INPUT_SIZE
        val scaleY = imgHeight.toFloat() / DET_INPUT_SIZE

        if (outputs == null) return detections

        try {
            val outputTensor = outputs.get(0)
            val rawValue = outputTensor.value

            Log.d(TAG, "检测输出类型: ${rawValue?.javaClass?.name}")

            val flatDetections = extractDetectionFloats(rawValue)

            if (flatDetections.isEmpty()) {
                Log.w(TAG, "未能从检测输出提取到任何数据")
                return detections
            }
            val stride = when {
                flatDetections.size % 15 == 0 -> 15
                flatDetections.size % 6 == 0 -> 6
                flatDetections.size % 5 == 0 -> 5
                else -> 0
            }

            if (stride == 0) {
                Log.w(TAG, "无法确定检测输出步长: 共 ${flatDetections.size} 个值")
                return detections
            }

            val count = flatDetections.size / stride
            Log.d(TAG, "检测输出: $count 行, 步长 $stride")

            val sampleScores = (0 until min(count, 5)).map { i -> flatDetections[i * stride + 4] }
            Log.d(TAG, "前5个检测框置信度采样: $sampleScores (阈值: $DET_CONF_THRESHOLD)")

            for (i in 0 until count) {
                val offset = i * stride
                var score = flatDetections[offset + 4]
                if (score < 0f || score > 1f) {
                    score = (1.0 / (1.0 + Math.exp(-score.toDouble()))).toFloat()
                }
                if (score <= DET_CONF_THRESHOLD) continue

                val x1 = (flatDetections[offset + 0] * scaleX).toInt().coerceIn(0, imgWidth)
                val y1 = (flatDetections[offset + 1] * scaleY).toInt().coerceIn(0, imgHeight)
                val x2 = (flatDetections[offset + 2] * scaleX).toInt().coerceIn(0, imgWidth)
                val y2 = (flatDetections[offset + 3] * scaleY).toInt().coerceIn(0, imgHeight)

                if (x2 <= x1 || y2 <= y1) continue
                val faceW = x2 - x1
                val faceH = y2 - y1
                if (faceW < 20 || faceH < 20) continue
                val aspectRatio = faceH.toFloat() / faceW.toFloat()
                if (aspectRatio < 0.3f || aspectRatio > 4.0f) continue

                val landmarks = mutableListOf<Pair<Float, Float>>()
                if (stride >= 15) {
                    for (k in 0 until 5) {
                        landmarks.add(
                            Pair(
                                flatDetections[offset + 5 + k * 2] * scaleX,
                                flatDetections[offset + 6 + k * 2] * scaleY
                            )
                        )
                    }
                }

                detections.add(
                    FaceDetection(
                        rect = Rect(x1, y1, x2, y2),
                        confidence = score,
                        landmarks = landmarks
                    )
                )
            }

            Log.d(TAG, "原始检测: ${count} 行, 通过阈值: ${detections.size}")

        } catch (e: Exception) {
            Log.e(TAG, "解析检测输出失败: ${e.javaClass.simpleName} - ${e.message}", e)
        }

        return detections
    }

    /**
     * 解析多步长分离输出 - 使用模型元数据按 lastDim 分类
     * lastDim=1 → score, lastDim=4 → bbox, lastDim=10 → kps
     * anchorCount → 估算 stride
     */
    private fun parseMultiStrideOutputs(
        outputs: OrtSession.Result,
        imgWidth: Int,
        imgHeight: Int
    ): List<FaceDetection> {
        data class StrideGroup(
            val stride: Int,
            val scoreIdx: Int = -1,
            val bboxIdx: Int = -1,
            val kpsIdx: Int = -1
        )

        val detections = mutableListOf<FaceDetection>()
        val outputSize = outputs.size()

        try {
            val scoreOutputs = mutableListOf<Pair<Int, Int>>()
            val bboxOutputs = mutableListOf<Pair<Int, Int>>()
            val kpsOutputs = mutableListOf<Pair<Int, Int>>()

            if (detOutputShapes.size == outputSize) {
                for ((i, shapeInfo) in detOutputShapes.withIndex()) {
                    Log.d(TAG, "输出[$i] '${shapeInfo.name}': anchorCount=${shapeInfo.anchorCount}, lastDim=${shapeInfo.lastDim}")
                    when (shapeInfo.lastDim) {
                        1 -> scoreOutputs.add(i to shapeInfo.anchorCount)
                        4 -> bboxOutputs.add(i to shapeInfo.anchorCount)
                        10 -> kpsOutputs.add(i to shapeInfo.anchorCount)
                        else -> Log.w(TAG, "输出[$i]: 未知 lastDim=${shapeInfo.lastDim}")
                    }
                }
            } else {
                Log.w(TAG, "缓存形状数量(${detOutputShapes.size}) != 输出数量($outputSize)，回退 flatSize 分类")
                for (i in 0 until outputSize) {
                    val flatArr = extractFlatFloatArray(outputs.get(i).value)
                    val flatSize = flatArr.size
                    when {
                        flatSize in intArrayOf(12800, 3200, 800, 200, 50) -> scoreOutputs.add(i to flatSize)
                        flatSize in intArrayOf(51200, 12800, 3200, 800, 200) -> bboxOutputs.add(i to flatSize / 4)
                        flatSize in intArrayOf(128000, 32000, 8000, 2000, 500) -> kpsOutputs.add(i to flatSize / 10)
                        else -> Log.d(TAG, "输出[$i]: flatSize=$flatSize 未匹配")
                    }
                }
            }

            Log.i(TAG, "输出分类: score=${scoreOutputs.size}, bbox=${bboxOutputs.size}, kps=${kpsOutputs.size}")

            val strideMap = mutableMapOf<Int, StrideGroup>()

            for ((idx, anchorCount) in scoreOutputs) {
                val stride = estimateStride(anchorCount)
                val existing = strideMap.getOrDefault(stride, StrideGroup(stride))
                strideMap[stride] = existing.copy(scoreIdx = idx)
            }

            for ((idx, anchorCount) in bboxOutputs) {
                val stride = estimateStride(anchorCount)
                val existing = strideMap.getOrDefault(stride, StrideGroup(stride))
                strideMap[stride] = existing.copy(bboxIdx = idx)
            }

            for ((idx, anchorCount) in kpsOutputs) {
                val stride = estimateStride(anchorCount)
                val existing = strideMap.getOrDefault(stride, StrideGroup(stride))
                strideMap[stride] = existing.copy(kpsIdx = idx)
            }

            val groups = strideMap.values.sortedBy { it.stride }
            for (g in groups) {
                Log.i(TAG, "步长${g.stride}: score=[${g.scoreIdx}], bbox=[${g.bboxIdx}], kps=[${if (g.kpsIdx >= 0) "${g.kpsIdx}" else "无"}]")
            }

            val scaleX = imgWidth.toFloat() / DET_INPUT_SIZE.toFloat()
            val scaleY = imgHeight.toFloat() / DET_INPUT_SIZE.toFloat()

            for (group in groups) {
                if (group.scoreIdx < 0 || group.bboxIdx < 0) {
                    Log.w(TAG, "步长${group.stride}: 缺少 score 或 bbox 输出，跳过")
                    continue
                }

                val scoreValues = extractFlatFloatArray(outputs.get(group.scoreIdx).value)
                val bboxValues = extractFlatFloatArray(outputs.get(group.bboxIdx).value)
                val numAnchors = scoreValues.size
                val bboxDim = if (numAnchors > 0) bboxValues.size / numAnchors else 0

                Log.d(TAG, "步长${group.stride}: anchors=$numAnchors, bboxDim=$bboxDim, " +
                    "scoreRange=[${"%.4f".format(scoreValues.minOrNull())}, ${"%.4f".format(scoreValues.maxOrNull())}]")

                if (numAnchors == 0 || bboxDim < 4) {
                    Log.w(TAG, "步长${group.stride}: 数据无效，跳过")
                    continue
                }

                val stride = group.stride
                val featSize = DET_INPUT_SIZE / stride
                val anchorsPerPos = numAnchors / (featSize * featSize)
                Log.d(TAG, "步长${group.stride}: anchorsPerPos=$anchorsPerPos")

                val kpsValues = if (group.kpsIdx >= 0) {
                    extractFlatFloatArray(outputs.get(group.kpsIdx).value)
                } else null
                val kpsPerBox = if (kpsValues != null && numAnchors > 0) kpsValues.size / numAnchors else 0

                var groupPassCount = 0
                for (idx in 0 until numAnchors) {
                    var score = scoreValues[idx]
                    if (score < 0f || score > 1f) {
                        score = (1.0 / (1.0 + Math.exp(-score.toDouble()))).toFloat()
                    }
                    if (score <= DET_CONF_THRESHOLD) continue

                    val posIdx = idx / anchorsPerPos
                    val gy = posIdx / featSize
                    val gx = posIdx % featSize
                    val cx = (gx + 0.5f) * stride
                    val cy = (gy + 0.5f) * stride

                    val bOff = idx * bboxDim
                    val rawLeft = bboxValues[bOff + 0]
                    val rawTop = bboxValues[bOff + 1]
                    val rawRight = bboxValues[bOff + 2]
                    val rawBottom = bboxValues[bOff + 3]

                    val useStrideScale = rawLeft in -10f..10f && rawRight in -10f..10f &&
                        rawTop in -10f..10f && rawBottom in -10f..10f
                    val s = if (useStrideScale) stride.toFloat() else 1f

                    val x1 = ((cx - rawLeft * s) * scaleX).toInt().coerceIn(0, imgWidth)
                    val y1 = ((cy - rawTop * s) * scaleY).toInt().coerceIn(0, imgHeight)
                    val x2 = ((cx + rawRight * s) * scaleX).toInt().coerceIn(0, imgWidth)
                    val y2 = ((cy + rawBottom * s) * scaleY).toInt().coerceIn(0, imgHeight)

                    if (x2 <= x1 || y2 <= y1) continue

                    val faceW = x2 - x1
                    val faceH = y2 - y1
                    if (faceW < 20 || faceH < 20) continue
                    val aspectRatio = faceH.toFloat() / faceW.toFloat()
                    if (aspectRatio < 0.3f || aspectRatio > 4.0f) continue

                    val landmarks = mutableListOf<Pair<Float, Float>>()
                    if (kpsValues != null && kpsPerBox >= 10) {
                        val kpsStride = if (useStrideScale) stride.toFloat() else 1f
                        for (k in 0 until 5) {
                            val kx = (cx + kpsValues[idx * kpsPerBox + k * 2] * kpsStride) * scaleX
                            val ky = (cy + kpsValues[idx * kpsPerBox + k * 2 + 1] * kpsStride) * scaleY
                            landmarks.add(Pair(kx, ky))
                        }
                    }

                    detections.add(
                        FaceDetection(
                            rect = Rect(x1, y1, x2, y2),
                            confidence = score.coerceIn(0f, 1f),
                            landmarks = landmarks
                        )
                    )
                    groupPassCount++
                }
                Log.d(TAG, "步长$stride: 通过阈值 $groupPassCount / $numAnchors")
            }

            Log.i(TAG, "多输出解析完成: ${detections.size} 张人脸")

        } catch (e: Exception) {
            Log.e(TAG, "多输出解析失败: ${e.javaClass.simpleName} - ${e.message}", e)
        }

        return detections
    }

    private fun estimateStride(anchorCount: Int): Int {
        val featSize = kotlin.math.sqrt(anchorCount.toDouble()).toInt()
        val knownSizes = intArrayOf(160, 80, 40, 20, 10)
        val best = knownSizes.minByOrNull { kotlin.math.abs(it - featSize) } ?: featSize
        return DET_INPUT_SIZE / best
    }

    private fun getTensorSample(value: Any?): String {
        if (value == null) return "null"
        return try {
            val floats = extractFlatFloatArray(value)
            val n = minOf(8, floats.size)
            floats.take(n).joinToString(", ") { "%.4f".format(it) } +
                if (floats.size > n) " ... (共${floats.size})" else ""
        } catch (_: Exception) { "?" }
    }

    private fun extractFlatFloatArray(value: Any?): FloatArray {
        if (value == null) return FloatArray(0)
        val result = mutableListOf<Float>()
        flattenToFloats(value, result)
        return result.toFloatArray()
    }

    private fun flattenToFloats(value: Any, result: MutableList<Float>) {
        when (value) {
            is FloatArray -> result.addAll(value.asList())
            is Float -> result.add(value)
            is Double -> result.add(value.toFloat())
            is DoubleArray -> { for (d in value) result.add(d.toFloat()) }
            is IntArray -> { for (i in value) result.add(i.toFloat()) }
            is LongArray -> { for (l in value) result.add(l.toFloat()) }
            is ShortArray -> { for (s in value) result.add(s.toFloat()) }
            is Number -> result.add(value.toFloat())
            is Array<*> -> {
                for (element in value) {
                    if (element != null) flattenToFloats(element, result)
                }
            }
            is java.nio.FloatBuffer -> {
                val buf = FloatArray(value.remaining())
                value.get(buf)
                result.addAll(buf.asList())
            }
            is java.nio.DoubleBuffer -> {
                val buf = DoubleArray(value.remaining())
                value.get(buf)
                for (d in buf) result.add(d.toFloat())
            }
            else -> {}
        }
    }

    /**
     * 从 ONNX tensor value 提取 float 数组，处理各种可能的返回类型
     */
    private fun extractDetectionFloats(rawValue: Any?): FloatArray {
        if (rawValue == null) return FloatArray(0)
        return extractFlatFloatArray(rawValue)
    }

    /**
     * 诊断扫描 - 列出所有输出中的 Top-10 最高分数
     * 用于排查模型是否在输出、分数是否在阈值附近
     */
    private fun diagnosticScanOutputs(outputs: OrtSession.Result, outputCount: Int) {
        data class ScoreEntry(val score: Float, val rawScore: Float, val outputIdx: Int, val anchorIdx: Int)

        val allScores = mutableListOf<ScoreEntry>()

        for (i in 0 until outputCount) {
            try {
                val flatArr = extractFlatFloatArray(outputs.get(i).value)
                if (flatArr.isEmpty()) continue

                val isScoreOutput = flatArr.size < 50000

                if (isScoreOutput) {
                    for (idx in flatArr.indices) {
                        var raw = flatArr[idx]
                        if (raw.isNaN() || raw.isInfinite()) continue
                        var prob = raw
                        if (prob < 0f || prob > 1f) {
                            prob = (1.0 / (1.0 + Math.exp(-raw.toDouble()))).toFloat()
                        }
                        allScores.add(ScoreEntry(prob, raw, i, idx))
                    }
                }
            } catch (_: Exception) {}
        }

        val top10 = allScores.sortedByDescending { it.score }.take(10)
        if (top10.isEmpty()) {
            Log.e(TAG, "诊断: 没有任何有效的分数数据！模型可能没有正确运行")
        } else {
            Log.w(TAG, "诊断: Top-10 最高分数:")
            for ((rank, entry) in top10.withIndex()) {
                Log.w(TAG, "  #${rank + 1}: prob=${"%.6f".format(entry.score)}, raw=${"%.4f".format(entry.rawScore)}, output=${entry.outputIdx}, anchor=${entry.anchorIdx}")
            }
            val maxScore = top10[0].score
            if (maxScore < 0.01f) {
                Log.e(TAG, "诊断: 最高分数 < 0.01 → 模型推理结果异常，输入可能无效或模型不兼容")
            } else if (maxScore < DET_CONF_THRESHOLD) {
                Log.w(TAG, "诊断: 最高分数 ${"%.4f".format(maxScore)} < 阈值 $DET_CONF_THRESHOLD → 检测阈值过高，将尝试临时降低阈值重新检测")
            }
        }
    }

    /**
     * NMS (非极大值抑制)
     */
    private fun applyNMS(detections: List<FaceDetection>, iouThreshold: Float): List<FaceDetection> {
        if (detections.size <= 1) return detections

        val sorted = detections.sortedByDescending { it.confidence }
        val keep = mutableListOf<FaceDetection>()
        val suppressed = BooleanArray(sorted.size)

        for (i in sorted.indices) {
            if (suppressed[i]) continue

            keep.add(sorted[i])

            for (j in i + 1 until sorted.size) {
                if (suppressed[j]) continue
                if (calculateIoU(sorted[i].rect, sorted[j].rect) > iouThreshold) {
                    suppressed[j] = true
                }
            }
        }

        return keep
    }

    /**
     * 计算两个矩形 IoU
     */
    private fun calculateIoU(a: Rect, b: Rect): Float {
        val x1 = max(a.left, b.left)
        val y1 = max(a.top, b.top)
        val x2 = min(a.right, b.right)
        val y2 = min(a.bottom, b.bottom)

        val intersection = max(0, x2 - x1) * max(0, y2 - y1)
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        val union = areaA + areaB - intersection

        return if (union > 0) intersection.toFloat() / union else 0f
    }

    /**
     * L2 归一化 - 原地计算减少内存分配
     */
    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sum = 0f
        for (v in vector) {
            sum += v * v
        }
        val norm = sqrt(sum)
        if (norm > 0) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        return vector
    }

    /**
     * 余弦相似度 - 同时处理归一化和未归一化向量
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom > 0f) dotProduct / denom else 0f
    }

    /**
     * 在图片上绘制检测结果
     */
    fun drawDetections(bitmap: Bitmap, detections: List<FaceDetection>, names: List<String>? = null): Bitmap {
        val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: throw Exception("无法转换 HARDWARE Bitmap")
        } else {
            bitmap
        }
        val resultBitmap = safeBitmap.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw Exception("无法复制 Bitmap (${safeBitmap.config}, ${safeBitmap.width}x${safeBitmap.height})")
        if (safeBitmap !== bitmap) safeBitmap.recycle()
        val canvas = Canvas(resultBitmap)

        val boxPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = max(3f, bitmap.width / 300f)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.GREEN
            textSize = max(24f, bitmap.width / 50f)
            isAntiAlias = true
            isFakeBoldText = true
        }

        val bgPaint = Paint().apply {
            color = Color.argb(160, 0, 0, 0)
            style = Paint.Style.FILL
        }

        val landmarkPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
            strokeWidth = 3f
        }

        detections.forEachIndexed { index, detection ->
            // 绘制检测框
            canvas.drawRect(detection.rect, boxPaint)

            // 绘制关键点
            detection.landmarks.forEach { (lx, ly) ->
                canvas.drawCircle(lx, ly, 4f, landmarkPaint)
            }

            // 绘制标签背景
            val label = names?.getOrNull(index) ?: "Face ${index + 1}"
            val scoreText = String.format("%.2f", detection.confidence)
            val fullText = "$label ($scoreText)"

            val textWidth = textPaint.measureText(fullText)
            val textHeight = textPaint.fontMetrics.run { descent - ascent }
            val padding = 6f

            val bgLeft = detection.rect.left.toFloat()
            val bgTop = detection.rect.top.toFloat() - textHeight - padding * 2
            val bgRight = bgLeft + textWidth + padding * 2
            val bgBottom = detection.rect.top.toFloat()

            canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, bgPaint)
            canvas.drawText(fullText, bgLeft + padding, bgBottom - padding, textPaint)
        }

        return resultBitmap
    }

    /**
     * 释放资源
     */
    fun close() {
        Log.i(TAG, "释放 ONNX Runtime 资源...")
        isLoaded = false
        try { detSession?.close() } catch (e: Exception) { Log.w(TAG, "关闭检测会话失败: ${e.message}") }
        try { recSession?.close() } catch (e: Exception) { Log.w(TAG, "关闭识别会话失败: ${e.message}") }
        detSession = null
        recSession = null
        try { env?.close() } catch (e: Exception) { Log.w(TAG, "关闭环境失败: ${e.message}") }
        env = null
        Log.i(TAG, "资源已释放")
    }

    data class FaceDetection(
        val rect: Rect,
        val confidence: Float,
        val landmarks: List<Pair<Float, Float>> = emptyList()
    )

    data class RecognitionResult(
        val name: String,
        val confidence: Float
    )
}
