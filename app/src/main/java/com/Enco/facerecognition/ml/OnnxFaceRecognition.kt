package com.Enco.facerecognition.ml // 声明当前文件所属的包路径，用于组织和引用项目中的类

// 导入 ONNX Runtime 的核心类，包括 OrtSession、OrtEnvironment、OnnxTensor 等
import ai.onnxruntime.*
// 导入 Android Context 类，用于访问应用资源和系统服务
import android.content.Context
// 导入 Android Bitmap 类，用于图像的内存表示和像素操作
import android.graphics.Bitmap
// 导入 Canvas 类，用于在 Bitmap 上绘制图形
import android.graphics.Canvas
// 导入 Color 类，用于定义颜色常量（如 GREEN、YELLOW 等）
import android.graphics.Color
// 导入 Matrix 类，用于二维图形变换（平移、旋转、缩放等）
import android.graphics.Matrix
// 导入 Paint 类，用于定义绘制样式（颜色、线宽、抗锯齿等）
import android.graphics.Paint
// 导入 Rect 类，用于表示矩形区域（人脸检测框）
import android.graphics.Rect
// 导入 Build 类，用于获取设备硬件信息（芯片型号、设备名称等）
import android.os.Build
// 导入 BuildConfig，用于区分 Debug/Release 构建，控制诊断日志输出
import com.Enco.facerecognition.BuildConfig
// 导入 Log 类，用于在 Logcat 中输出调试和错误日志
import android.util.Log
// 导入 Dispatchers，用于指定协程运行的线程调度器
import kotlinx.coroutines.Dispatchers
// 导入 withContext 函数，用于在协程中切换线程上下文
import kotlinx.coroutines.withContext
// 并行处理：async/coroutineScope 用于多张人脸并行，Semaphore 限制 ONNX 推理并发
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
// Mutex：保护 detSession.run / recSession.run，OrtSession.run 非线程安全
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
// 导入 File 类，用于文件系统操作（模型缓存文件的读写）
import java.io.File
// 导入 FileOutputStream 类，用于将模型文件从 assets 复制到缓存目录
import java.io.FileOutputStream
// 导入 FloatBuffer 类，用于创建 ONNX 张量时传递浮点数据
import java.nio.FloatBuffer
// 导入 atan2 函数，用于计算反正切值（相似变换中的角度计算）
import kotlin.math.atan2
// 导入 cos 函数，用于计算余弦值（相似变换中的旋转矩阵）
import kotlin.math.cos
// 导入 max 函数，用于返回两个值中的较大值
import kotlin.math.max
// 导入 min 函数，用于返回两个值中的较小值
import kotlin.math.min
// 导入 sin 函数，用于计算正弦值（相似变换中的旋转矩阵）
import kotlin.math.sin
// 导入 sqrt 函数，用于计算平方根（L2 归一化和余弦相似度计算）
import kotlin.math.sqrt

/**
 * ONNX Runtime 人脸识别引擎
 * 针对骁龙 8 系列旗舰芯片深度优化
 */
// 定义 OnnxFaceRecognition 类，接收 Android Context 作为构造参数
class OnnxFaceRecognition(context: Context) { // 声明类构造函数，传入应用上下文

    // 声明伴生对象，包含类级别的常量和静态成员
    companion object {
        // 定义日志标签，用于 Logcat 过滤本类的日志输出
        private const val TAG = "OnnxFaceRecognition" // 日志过滤标签

        // 定义人脸检测模型的文件名（SCRFD 模型）
        const val DETECTION_MODEL = "det_10g.onnx" // SCRFD 检测模型文件名
        // 定义人脸识别模型的文件名（ArcFace 模型）
        const val RECOGNITION_MODEL = "w600k_r50.onnx" // ArcFace 识别模型文件名

        // 定义检测模型输入图像尺寸为 640x640 像素
        const val DET_INPUT_SIZE = 640 // 检测模型输入尺寸（宽和高）
        // 定义识别模型输入图像尺寸为 112x112 像素
        const val REC_INPUT_SIZE = 112 // 识别模型输入尺寸（宽和高）
        // 定义人脸特征向量的维度为 512
        const val EMBEDDING_DIM = 512 // 特征嵌入向量维度

        // 定义检测置信度阈值，低于此值的检测结果将被过滤
        const val DET_CONF_THRESHOLD = 0.5f // 检测置信度阈值
        // 定义 NMS（非极大值抑制）的 IoU 阈值，用于去除重叠框
        const val DET_IOU_THRESHOLD = 0.45f // NMS 的 IoU 交并比阈值

        // 模型文件预期大小（用于校验）
        // 定义检测模型的预期文件大小（单位 MB）
        const val DET_MODEL_SIZE_MB = 16L // 检测模型预期大小（MB）
        // 定义识别模型的预期文件大小（单位 MB）
        const val REC_MODEL_SIZE_MB = 166L // 识别模型预期大小（MB）

        // 骁龙 8 系列最优线程数
        // 定义骁龙 8 系列芯片的最优推理线程数
        const val SD8_OPTIMAL_THREADS = 6 // 骁龙 8 系列推荐线程数
    } // 结束伴生对象

    // 保存应用上下文的引用，避免内存泄漏
    private val appContext = context.applicationContext // 获取应用级别的上下文
    // 检测模型的 ONNX 推理会话，初始为 null
    private var detSession: OrtSession? = null // 检测模型推理会话
    // 识别模型的 ONNX 推理会话，初始为 null
    private var recSession: OrtSession? = null // 识别模型推理会话
    // ONNX Runtime 运行环境，初始为 null
    private var env: OrtEnvironment? = null // ONNX Runtime 运行环境实例
    // 检测模型的输入张量名称，默认为 "input"
    private var detInputName: String = "input" // 检测模型输入节点名称
    // 识别模型的输入张量名称，默认为 "input"
    private var recInputName: String = "input" // 识别模型输入节点名称

    // 定义输出形状信息的数据类，存储输出名称、最后一维大小和锚点数量
    private data class OutputShapeInfo(val name: String, val lastDim: Int, val anchorCount: Int) // 输出张量形状信息
    // 缓存检测模型各输出层的形状信息列表
    private var detOutputShapes: List<OutputShapeInfo> = emptyList() // 检测模型输出形状缓存

    // === ONNX 推理互斥锁 ===
    // OrtSession.run 非线程安全（见 AGENTS.md），所有 run() 调用必须持锁。
    // 用单一 Mutex 同时保护检测和识别会话，避免 VideoProcessor 多消费者并发崩溃。
    private val inferenceMutex = Mutex() // ONNX 推理互斥锁

    // 缓冲区大小常量
    // 检测输入的像素缓冲区大小（640*640=409600 个像素）
    private val detPixelBufferSize = DET_INPUT_SIZE * DET_INPUT_SIZE // 检测像素缓冲区大小
    // 检测输入的浮点缓冲区大小（3 通道 * 640*640）
    private val detFloatBufferSize = 3 * DET_INPUT_SIZE * DET_INPUT_SIZE // 检测浮点缓冲区大小（CHW 格式）
    // 识别输入的像素缓冲区大小（112*112=12544 个像素）
    private val recPixelBufferSize = REC_INPUT_SIZE * REC_INPUT_SIZE // 识别像素缓冲区大小
    // 识别输入的浮点缓冲区大小（3 通道 * 112*112）
    private val recFloatBufferSize = 3 * REC_INPUT_SIZE * REC_INPUT_SIZE // 识别浮点缓冲区大小（CHW 格式）

    // 设备信息
    // 检测当前设备是否为骁龙 8 系列芯片
    private val isSnapdragon8 = isSnapdragon8Series() // 判断是否为骁龙 8 系列设备
    // 根据设备型号选择最优线程数：骁龙 8 用 6 线程，其他设备取 CPU 核心数一半（上限 4）
    private val optimalThreads = if (isSnapdragon8) SD8_OPTIMAL_THREADS else max(1, min(4, Runtime.getRuntime().availableProcessors() / 2)) // 计算最优推理线程数

    // 公开的模型加载状态标志，外部可读但只能在类内部修改
    var isLoaded = false // 模型是否已成功加载的标志
        private set // 仅允许类内部设置该属性

    // 初始化块，在类实例化时自动执行
    init {
        // 尝试初始化模型
        try {
            // 调用模型初始化方法
            initModels()
        // 捕获初始化过程中的任何异常
        } catch (e: Exception) {
            // 记录模型初始化失败的错误日志
            Log.e(TAG, "模型初始化失败: ${e.message}", e)
        } // 结束异常捕获
    } // 结束初始化块

    // 私有方法：初始化所有 ONNX 模型
    private fun initModels() {
        // 打印初始化开始的分隔线
        Log.i(TAG, "========================================")
        // 打印 ONNX Runtime 初始化开始日志
        Log.i(TAG, "开始初始化 ONNX Runtime")
        // 打印设备型号信息（骁龙 8 系列或其他）
        Log.i(TAG, "设备: ${if (isSnapdragon8) "骁龙 8 系列" else "其他"}")
        // 打印将使用的推理线程数
        Log.i(TAG, "线程数: $optimalThreads")
        // 打印初始化信息结束的分隔线
        Log.i(TAG, "========================================")
        // 记录初始化开始的时间戳，用于计算耗时
        val startTime = System.currentTimeMillis()

        // 检查 assets 目录中的文件
        // 尝试列出 assets 目录下的所有文件
        try {
            // 获取 assets 根目录下的文件列表
            val assets = appContext.assets.list("")
            // 检查文件列表是否不为 null
            if (assets != null) {
                // 打印 assets 目录文件列表的标题
                Log.d(TAG, "Assets 目录文件列表:")
                // 遍历所有文件并逐个打印文件名
                assets.forEach { fileName ->
                    // 打印当前文件名
                    Log.d(TAG, "  - $fileName")
                } // 结束遍历

                // 检查检测模型文件是否存在于 assets 中
                if (!assets.contains(DETECTION_MODEL)) {
                    // 如果检测模型缺失，打印警告日志
                    Log.e(TAG, "⚠️ 检测模型缺失: $DETECTION_MODEL")
                } // 结束检测模型检查
                // 检查识别模型文件是否存在于 assets 中
                if (!assets.contains(RECOGNITION_MODEL)) {
                    // 如果识别模型缺失，打印警告日志
                    Log.e(TAG, "⚠️ 识别模型缺失: $RECOGNITION_MODEL")
                } // 结束识别模型检查
            // 如果文件列表为 null
            } else {
                // 打印 assets 目录为空或无法读取的警告
                Log.w(TAG, "⚠️ Assets 目录为空或无法读取")
            } // 结束文件列表检查
        // 捕获检查 assets 目录时的异常
        } catch (e: Exception) {
            // 打印检查 assets 目录失败的警告日志
            Log.w(TAG, "⚠️ 检查 Assets 目录失败: ${e.message}")
        } // 结束 assets 目录检查

        // 获取 ONNX Runtime 全局环境实例
        env = OrtEnvironment.getEnvironment()
        // 打印环境初始化成功的日志
        Log.i(TAG, "ONNX Runtime 环境初始化成功")

        // 创建检测模型的会话配置选项
        val detSessionOptions = OrtSession.SessionOptions().apply {
            // 启用内存模式优化，减少内存分配
            setMemoryPatternOptimization(true)
            // 设置优化等级为最高级别 ALL_OPT
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            // 设置算子内部并行线程数
            setIntraOpNumThreads(optimalThreads)
            // 设置算子之间并行线程数
            setInterOpNumThreads(optimalThreads)

            // 🚀 启用 NNAPI 硬件加速（Android 8.1+）
            // 注意：必须调用 addNnapi() 扩展函数才能正确启用 NNAPI，
            //      旧代码用 addConfigEntry("NNAPI_FLAG","1") 是无效的（ONNX Runtime 不识别该键）。
            //      NNAPI 会自动调用设备的 GPU/NPU/DSP 进行推理，骁龙 8 系列可达 2-5x 加速。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                try {
                    addNnapi()  // 官方扩展函数，正确启用 NNAPI
                    Log.i(TAG, "✅ NNAPI 硬件加速已启用（检测模型）")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ NNAPI 不可用，回退 CPU 推理: ${e.message}")
                }
            } else {
                Log.i(TAG, "ℹ️ 系统 < 8.1，NNAPI 不可用，使用 CPU 推理")
            }
        } // 结束检测模型会话配置

        // 创建识别模型的会话配置选项
        val recSessionOptions = OrtSession.SessionOptions().apply {
            // 启用内存模式优化，减少内存分配
            setMemoryPatternOptimization(true)
            // 设置优化等级为最高级别 ALL_OPT
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            // 设置算子内部并行线程数
            setIntraOpNumThreads(optimalThreads)
            // 设置算子之间并行线程数
            setInterOpNumThreads(optimalThreads)

            // 🚀 启用 NNAPI 硬件加速（Android 8.1+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                try {
                    addNnapi()  // 官方扩展函数
                    Log.i(TAG, "✅ NNAPI 硬件加速已启用（识别模型）")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ NNAPI 不可用，回退 CPU 推理: ${e.message}")
                }
            }
        } // 结束识别模型会话配置

        // 尝试加载检测模型
        try {
            // 加载检测模型
            // 打印加载检测模型的分隔线
            Log.i(TAG, "----------------------------------------")
            // 打印开始加载检测模型的日志
            Log.i(TAG, "加载检测模型...")
            // 从 assets 复制检测模型到缓存目录并获取文件引用
            val detModelFile = copyModelFromAssets(DETECTION_MODEL, DET_MODEL_SIZE_MB)
            // 检查模型文件是否存在且大小合法
            if (detModelFile != null && validateModelFile(detModelFile, DET_MODEL_SIZE_MB)) {
                // 尝试创建检测模型的推理会话
                try {
                    // 使用模型文件路径和配置创建 ONNX 推理会话
                    detSession = env?.createSession(detModelFile.absolutePath, detSessionOptions)
                    // 获取检测模型的第一个输入节点名称
                    detInputName = detSession?.inputNames?.firstOrNull() ?: "input"
                    // 打印检测模型加载成功的日志
                    Log.i(TAG, "✅ 检测模型加载成功")
                    // 获取所有输入名称并用逗号连接成字符串
                    val inputNames = detSession?.inputNames?.joinToString(", ")
                    // 打印检测模型的输入节点名称
                    Log.d(TAG, "检测模型输入: $inputNames (使用: $detInputName)")
                    // 获取所有输出名称并用逗号连接成字符串
                    val outputNames = detSession?.outputNames?.joinToString(", ")
                    // 打印检测模型的输出节点名称
                    Log.d(TAG, "检测模型输出: $outputNames")
                    // 尝试读取模型的详细 I/O 信息
                    try {
                        // 遍历检测模型的所有输入节点信息
                        detSession?.inputInfo?.forEach { (name, nodeInfo) ->
                            // 打印每个输入节点的名称和类型信息
                            Log.i(TAG, "  输入 '$name': ${nodeInfo.info}")
                        } // 结束输入信息遍历
                        // 遍历检测模型的所有输出节点信息
                        detSession?.outputInfo?.forEach { (name, nodeInfo) ->
                            // 打印每个输出节点的名称和类型信息
                            Log.i(TAG, "  输出 '$name': ${nodeInfo.info}")
                        } // 结束输出信息遍历
                        // 缓存检测模型各输出层的形状信息
                        detOutputShapes = detSession?.outputInfo?.map { (name, nodeInfo) ->
                            // 获取节点的类型信息
                            val info = nodeInfo.info
                            // 检查是否为张量类型信息
                            if (info is TensorInfo) {
                                // 获取张量的形状数组
                                val shape = info.shape
                                // 获取最后一维的大小（如边界框维度或关键点维度）
                                val lastDim = if (shape.isNotEmpty()) shape.last().toInt() else 0
                                // 获取锚点数量（形状的第一维）
                                val anchorCount = if (shape.size >= 2) shape.first().toInt() else 0
                                // 创建并返回输出形状信息对象
                                OutputShapeInfo(name, lastDim, anchorCount)
                            // 如果不是张量类型
                            } else {
                                // 返回默认的输出形状信息（维度为 0）
                                OutputShapeInfo(name, 0, 0)
                            } // 结束类型判断
                        // 如果为空则返回空列表
                        } ?: emptyList()
                        // 打印缓存的输出形状信息摘要
                        Log.i(TAG, "  缓存输出形状: ${detOutputShapes.map { "${it.name}[${it.anchorCount},${it.lastDim}]" }}")
                    // 捕获读取 I/O 信息时的异常
                    } catch (e: Exception) {
                        // 打印读取模型 I/O 信息失败的警告
                        Log.w(TAG, "读取模型I/O信息失败: ${e.message}")
                    } // 结束 I/O 信息读取的异常捕获
                // 捕获创建检测会话时的异常
                } catch (e: Exception) {
                    // 打印创建检测模型会话失败的错误日志
                    Log.e(TAG, "❌ 创建检测模型会话失败: ${e.javaClass.simpleName} - ${e.message}")
                    // 打印异常的堆栈跟踪信息
                    e.printStackTrace()
                } // 结束创建检测会话的异常捕获
            // 如果模型文件无效或不存在
            } else {
                // 打印检测模型加载失败的错误日志
                Log.e(TAG, "❌ 检测模型加载失败")
            } // 结束模型文件验证
        // 无论是否成功都执行的清理块
        } finally {
            // 关闭检测模型的会话配置选项，释放资源
            detSessionOptions.close()
        } // 结束检测模型加载的 try-finally 块

        // 尝试加载识别模型
        try {
            // 加载识别模型
            // 打印加载识别模型的分隔线
            Log.i(TAG, "----------------------------------------")
            // 打印开始加载识别模型的日志
            Log.i(TAG, "加载识别模型...")
            // 从 assets 复制识别模型到缓存目录并获取文件引用
            val recModelFile = copyModelFromAssets(RECOGNITION_MODEL, REC_MODEL_SIZE_MB)
            // 检查模型文件是否存在且大小合法
            if (recModelFile != null && validateModelFile(recModelFile, REC_MODEL_SIZE_MB)) {
                // 尝试创建识别模型的推理会话
                try {
                    // 使用模型文件路径和配置创建 ONNX 推理会话
                    recSession = env?.createSession(recModelFile.absolutePath, recSessionOptions)
                    // 获取识别模型的第一个输入节点名称
                    recInputName = recSession?.inputNames?.firstOrNull() ?: "input"
                    // 打印识别模型加载成功的日志
                    Log.i(TAG, "✅ 识别模型加载成功")
                    // 获取所有输入名称并用逗号连接成字符串
                    val inputNames = recSession?.inputNames?.joinToString(", ")
                    // 打印识别模型的输入节点名称
                    Log.d(TAG, "识别模型输入: $inputNames (使用: $recInputName)")
                    // 获取所有输出名称并用逗号连接成字符串
                    val outputNames = recSession?.outputNames?.joinToString(", ")
                    // 打印识别模型的输出节点名称
                    Log.d(TAG, "识别模型输出: $outputNames")
                    // 尝试读取模型的详细 I/O 信息
                    try {
                        // 遍历识别模型的所有输入节点信息
                        recSession?.inputInfo?.forEach { (name, nodeInfo) ->
                            // 打印每个输入节点的名称和类型信息
                            Log.i(TAG, "  输入 '$name': ${nodeInfo.info}")
                        } // 结束输入信息遍历
                        // 遍历识别模型的所有输出节点信息
                        recSession?.outputInfo?.forEach { (name, nodeInfo) ->
                            // 打印每个输出节点的名称和类型信息
                            Log.i(TAG, "  输出 '$name': ${nodeInfo.info}")
                        } // 结束输出信息遍历
                    // 捕获读取 I/O 信息时的异常
                    } catch (e: Exception) {
                        // 打印读取模型 I/O 信息失败的警告
                        Log.w(TAG, "读取模型I/O信息失败: ${e.message}")
                    } // 结束 I/O 信息读取的异常捕获
                // 捕获创建识别会话时的异常
                } catch (e: Exception) {
                    // 打印创建识别模型会话失败的错误日志
                    Log.e(TAG, "❌ 创建识别模型会话失败: ${e.javaClass.simpleName} - ${e.message}")
                    // 打印异常的堆栈跟踪信息
                    e.printStackTrace()
                } // 结束创建识别会话的异常捕获
            // 如果模型文件无效或不存在
            } else {
                // 打印识别模型加载失败的错误日志
                Log.e(TAG, "❌ 识别模型加载失败")
            } // 结束模型文件验证
        // 无论是否成功都执行的清理块
        } finally {
            // 关闭识别模型的会话配置选项，释放资源
            recSessionOptions.close()
        } // 结束识别模型加载的 try-finally 块

        // 判断是否所有模型都加载成功（检测和识别会话都不为 null）
        isLoaded = detSession != null && recSession != null
        // 计算初始化过程的总耗时（毫秒）
        val elapsed = System.currentTimeMillis() - startTime

        // 打印初始化结果的分隔线
        Log.i(TAG, "========================================")
        // 检查模型是否全部加载成功
        if (isLoaded) {
            // 打印所有模型加载完成的日志，附带耗时
            Log.i(TAG, "✅ 所有模型加载完成，耗时 ${elapsed}ms")
        // 如果有模型加载失败
        } else {
            // 打印部分模型加载失败的错误日志
            Log.e(TAG, "❌ 部分模型加载失败")
        } // 结束加载状态判断
        // 打印初始化结束的分隔线
        Log.i(TAG, "========================================")
    } // 结束 initModels 方法

    /**
     * 检测是否为骁龙 8 系列
     */
    // 私有方法：通过读取设备硬件信息判断是否为骁龙 8 系列芯片
    private fun isSnapdragon8Series(): Boolean {
        // 获取硬件标识符并转为小写
        val hardware = Build.HARDWARE.lowercase()
        // 获取主板标识符并转为小写
        val board = Build.BOARD.lowercase()
        // 获取设备标识符并转为小写
        val device = Build.DEVICE.lowercase()
        // 将所有标识符拼接成一个字符串用于关键词匹配
        val check = hardware + board + device
        // 检查是否包含骁龙 8 系列的已知标识符（sm8、sdm8、taro、kalama、pineapple 等）
        return check.contains("sm8") || check.contains("sdm8") || check.contains("taro") ||
               check.contains("kalama") || check.contains("pineapple") ||
               // 或者同时包含 "snapdragon" 和 "gen" 关键词（用于骁龙 8 Gen 系列）
               (check.contains("snapdragon") && check.contains("gen"))
    } // 结束 isSnapdragon8Series 方法

    // 私有方法：验证模型文件大小是否符合预期
    private fun validateModelFile(file: File, expectedSizeMb: Long): Boolean {
        // 将预期大小从 MB 转换为字节
        val expectedBytes = expectedSizeMb * 1024 * 1024
        // 检查文件大小是否达到预期大小的 80%（允许一定误差）
        val isValid = file.length() >= expectedBytes * 0.8
        // 如果文件大小不合法
        if (!isValid) {
            // 计算文件实际大小（MB）
            val sizeMb = file.length() / (1024 * 1024)
            // 打印模型文件大小异常的警告日志
            Log.w(TAG, "模型文件大小异常: ${file.name} = ${sizeMb}MB (预期 >= ${expectedSizeMb * 0.8}MB)")
        } // 结束大小校验
        // 返回文件是否有效的布尔值
        return isValid
    } // 结束 validateModelFile 方法

    /**
     * 从 assets 复制模型到缓存目录
     */
    // 私有方法：从 assets 目录复制模型文件到应用缓存目录，返回缓存文件或 null
    private fun copyModelFromAssets(modelName: String, expectedSizeMb: Long = 0L): File? {
        // 使用 try-catch 包裹整个复制过程
        return try {
            // 构造缓存目录中的输出文件路径
            val outFile = File(appContext.cacheDir, modelName)

            // 如果缓存文件已存在且大小合法，直接返回缓存文件（跳过复制）
            if (outFile.exists() && outFile.length() > expectedSizeMb * 1024 * 1024 * 0.8) {
                // 打印模型已缓存的日志
                Log.d(TAG, "模型已缓存: ${outFile.absolutePath}")
                // 直接返回已缓存的文件
                return outFile
            } // 结束缓存检查

            // 以 use 模式打开 assets 中的模型文件输入流（自动关闭）
            appContext.assets.open(modelName).use { input ->
                // 以 use 模式打开文件输出流（自动关闭）
                FileOutputStream(outFile).use { output ->
                    // 创建 8KB 的缓冲区用于流式复制
                    val buffer = ByteArray(8192)
                    // 声明变量记录每次读取的字节数
                    var bytesRead: Int
                    // 循环读取输入流数据直到结束（-1）
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // 将读取到的数据写入输出文件
                        output.write(buffer, 0, bytesRead)
                    } // 结束读写循环
                    // 强制将缓冲区数据写入磁盘
                    output.flush()
                } // 结束输出流 use 块
            } // 结束输入流 use 块

            // 打印模型复制完成的日志，附带文件路径和大小
            Log.i(TAG, "模型复制完成: ${outFile.absolutePath} (${outFile.length() / 1024 / 1024}MB)")
            // 返回复制后的文件对象
            outFile

        // 捕获复制过程中的任何异常
        } catch (e: Exception) {
            // 打印复制模型失败的错误日志
            Log.e(TAG, "复制模型失败 $modelName: ${e.message}")
            // 返回 null 表示复制失败
            null
        } // 结束异常捕获
    } // 结束 copyModelFromAssets 方法

    /**
     * 检测人脸 - 自动识别单/多输出格式
     */
    // 挂起函数：在后台线程中执行人脸检测，返回检测到的人脸列表
    suspend fun detectFaces(bitmap: Bitmap, detectionThreshold: Float = DET_CONF_THRESHOLD): List<FaceDetection> = withContext(Dispatchers.Default) {
        // 检查检测模型是否已加载，未加载则直接返回空列表
        if (detSession == null || env == null) {
            // 打印检测模型未加载的错误日志
            Log.e(TAG, "检测模型未加载")
            // 返回空列表并退出协程
            return@withContext emptyList()
        } // 结束模型加载检查

        // 声明输入张量变量，用于后续 finally 块中释放资源
        var inputTensor: OnnxTensor? = null
        // 声明输出结果变量，用于后续 finally 块中释放资源
        var outputs: OrtSession.Result? = null

        // 尝试执行检测推理
        try {
            // 记录检测开始的时间戳
            val startTime = System.currentTimeMillis()

            // 对输入图像进行预处理（缩放、归一化、转为张量）
            inputTensor = preprocessDetection(bitmap)
            // 运行检测模型推理，将输入张量传入模型
            // ⚠️ OrtSession.run 非线程安全，必须持 inferenceMutex。
            //   VideoProcessor 多消费者并发调用本方法时由 Mutex 串行化，避免 native 崩溃。
            outputs = inferenceMutex.withLock {
                detSession?.run(mapOf(detInputName to inputTensor))
            }

            // 获取模型输出的张量数量
            val outputCount = outputs?.size() ?: 0
            // 仅在 Debug 构建下打印详细输出张量信息，Release 不执行避免 JNI 开销
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "模型输出张量数: $outputCount")
            }

            // 仅在 Debug 构建下遍历输出张量做采样诊断
            // Release 构建跳过：extractFlatFloatArray 递归展平对大张量(12800+)消耗可观 CPU
            if (BuildConfig.DEBUG && outputCount > 0) {
                // 遍历前 5 个输出张量（减少日志量，原为 20）
                for (i in 0 until min(outputCount, 5)) {
                    // 尝试读取并打印每个输出的采样数据
                    try {
                        // 获取第 i 个输出张量的原始值
                        val rawVal = outputs!!.get(i).value
                        // 将原始值展平为一维浮点数组
                        val flatArr = extractFlatFloatArray(rawVal)
                        // 取前 10 个值作为采样
                        val n = minOf(10, flatArr.size)
                        // 将采样值格式化为逗号分隔的字符串
                        val sample = flatArr.take(n).joinToString(", ") { "%.6f".format(it) }
                        // 获取数组中的最小值
                        val minVal = if (flatArr.isNotEmpty()) flatArr.minOrNull() else 0f
                        // 获取数组中的最大值
                        val maxVal = if (flatArr.isNotEmpty()) flatArr.maxOrNull() else 0f
                        // 打印输出张量的详细信息（大小、范围、采样值），Debug 级别
                        Log.v(TAG, "  输出[$i]: flatSize=${flatArr.size}, range=[${"%.4f".format(minVal)}, ${"%.4f".format(maxVal)}], sample=[$sample]")
                    // 捕获读取单个输出时的异常
                    } catch (e: Exception) {
                        // 打印读取输出失败的警告
                        Log.w(TAG, "  输出[$i]: 读取失败 - ${e.message}")
                    } // 结束输出读取的异常捕获
                } // 结束输出遍历
            } // 结束输出数量检查

            // 根据输出数量选择解析策略：多输出用多步长解析，单输出用简单解析
            var detections = if (outputCount >= 2) {
                // 使用多步长分离输出解析策略（适用于 SCRFD 的多层输出）
                parseMultiStrideOutputs(outputs!!, bitmap.width, bitmap.height, detectionThreshold)
            // 如果只有单个输出
            } else {
                // 使用单输出解析策略
                parseDetectionOutputs(outputs, bitmap.width, bitmap.height, detectionThreshold)
            } // 结束解析策略选择

            // 如果主解析策略返回 0 个检测结果
            if (detections.isEmpty() && outputCount >= 1) {
                // 打印尝试回退策略的日志
                Log.w(TAG, "主解析策略返回0，尝试回退单输出解析...")
                // 使用单输出解析作为回退方案
                detections = parseDetectionOutputs(outputs, bitmap.width, bitmap.height, detectionThreshold)
            } // 结束回退策略

            // 如果所有解析策略都返回 0 个结果，且为 Debug 构建，执行诊断扫描
            if (detections.isEmpty() && outputCount > 0 && BuildConfig.DEBUG) {
                // 打印执行诊断扫描的日志
                Log.w(TAG, "所有解析策略返回0，执行诊断扫描...")
                // 执行诊断扫描，打印所有输出中的 Top-10 最高分数
                diagnosticScanOutputs(outputs!!, outputCount)
            } // 结束诊断扫描

            // 对检测结果应用非极大值抑制（NMS），去除重叠框
            val nmsDetections = applyNMS(detections, DET_IOU_THRESHOLD)

            // 计算检测过程的总耗时
            val elapsed = System.currentTimeMillis() - startTime
            // 打印检测完成的日志，包含检测到的人脸数和耗时
            Log.i(TAG, "检测完成: ${nmsDetections.size} 张人脸, 耗时 ${elapsed}ms")

            // 返回经 NMS 过滤后的检测结果列表
            nmsDetections

        // 捕获检测过程中的任何异常
        } catch (e: Exception) {
            // 打印检测失败的错误日志
            Log.e(TAG, "检测失败: ${e.message}", e)
            // 返回空列表表示检测失败
            emptyList()
        // 无论成功与否都释放资源
        } finally {
            // 安全关闭输入张量，忽略可能的异常
            try { inputTensor?.close() } catch (_: Exception) {}
            // 安全关闭输出结果，忽略可能的异常
            try { outputs?.close() } catch (_: Exception) {}
        } // 结束资源清理
    } // 结束 detectFaces 方法

    /**
     * 人脸对齐 - 使用5点关键点最小二乘相似变换
     * ArcFace 标准对齐目标点 (112x112)
     */
    // 公开方法：对检测到的人脸进行对齐变换，返回 112x112 的对齐后人脸图像
    fun alignFace(sourceBitmap: Bitmap, detection: FaceDetection): Bitmap {
        // 检查检测结果是否包含足够的人脸关键点（至少 2 个点）
        if (detection.landmarks.size < 2) {
            // 如果关键点不足，使用带边距的裁剪作为回退方案
            return cropFaceWithMargin(sourceBitmap, detection.rect)
        } // 结束关键点检查

        // 定义 ArcFace 标准对齐目标点坐标（5 个关键点，112x112 坐标系）
        val dstPoints = floatArrayOf(
            38.2946f, 51.6963f,  // 左眼中心坐标
            73.5318f, 51.5014f,  // 右眼中心坐标
            56.0252f, 71.7366f,  // 鼻尖坐标
            41.5493f, 92.3655f,  // 左嘴角坐标
            70.7299f, 92.2041f   // 右嘴角坐标
        ) // 结束目标关键点数组

        // 取检测到的关键点数量和 5 中的较小值
        val numPoints = minOf(detection.landmarks.size, 5)
        // 创建源关键点坐标数组（每个点有 x 和 y 两个值）
        val srcPoints = FloatArray(numPoints * 2)
        // 遍历所有关键点
        for (i in 0 until numPoints) {
            // 将第 i 个关键点的 x 坐标存入数组
            srcPoints[i * 2] = detection.landmarks[i].first
            // 将第 i 个关键点的 y 坐标存入数组
            srcPoints[i * 2 + 1] = detection.landmarks[i].second
        } // 结束源关键点填充

        // 通过最小二乘法估计相似变换矩阵
        val matrix = estimateSimilarityTransform(srcPoints, dstPoints, numPoints)

        // 创建 112x112 的目标 Bitmap 用于存储对齐后的人脸
        val alignedBitmap = Bitmap.createBitmap(REC_INPUT_SIZE, REC_INPUT_SIZE, Bitmap.Config.ARGB_8888)
        // 创建画布对象，绑定到目标 Bitmap
        val canvas = Canvas(alignedBitmap)
        // 创建画笔对象并设置绘制属性
        val paint = Paint().apply {
            // 启用抗锯齿使图像边缘更平滑
            isAntiAlias = true
            // 启用双线性过滤使缩放更平滑
            isFilterBitmap = true
        } // 结束画笔设置
        // 使用变换矩阵将源图像绘制到目标画布上，完成人脸对齐
        canvas.drawBitmap(sourceBitmap, matrix, paint)

        // 返回对齐后的人脸 Bitmap
        return alignedBitmap
    } // 结束 alignFace 方法

    // 私有方法：通过最小二乘法估计相似变换矩阵（旋转 + 缩放 + 平移）
    private fun estimateSimilarityTransform(
        src: FloatArray, dst: FloatArray, numPoints: Int // 源点坐标、目标点坐标、点数量
    ): Matrix {
        // 设置点的数量
        val n = numPoints
        // 提取所有源点的 x 坐标作为矩阵 A 的第一列
        val a11 = FloatArray(n) { i -> src[i * 2] }
        // 提取所有源点的 -y 坐标作为矩阵 A 的第二列（相似变换的旋转分量）
        val a12 = FloatArray(n) { i -> -src[i * 2 + 1] }
        // 矩阵 A 的第三列全为 1（平移 x 分量的系数）
        val a13 = FloatArray(n) { 1f }
        // 矩阵 A 的第四列全为 0
        val a14 = FloatArray(n) { 0f }
        // 提取所有源点的 y 坐标作为矩阵 A 的第五行对应列
        val a21 = FloatArray(n) { i -> src[i * 2 + 1] }
        // 提取所有源点的 x 坐标作为旋转矩阵的另一分量
        val a22 = FloatArray(n) { i -> src[i * 2] }
        // 矩阵的第七列全为 0
        val a23 = FloatArray(n) { 0f }
        // 矩阵的第八列全为 1（平移 y 分量的系数）
        val a24 = FloatArray(n) { 1f }
        // 提取所有目标点的 x 坐标
        val bx = FloatArray(n) { i -> dst[i * 2] }
        // 提取所有目标点的 y 坐标
        val by = FloatArray(n) { i -> dst[i * 2 + 1] }

        // 初始化 4x4 的法方程矩阵 A^T * A
        val ata = Array(4) { FloatArray(4) }
        // 初始化法方程右侧向量 A^T * b
        val atb = FloatArray(4)

        // 构建法方程：计算 A^T * A 和 A^T * b
        for (r in 0 until 4) { // 遍历法方程矩阵的每一行
            for (c in 0 until 4) { // 遍历法方程矩阵的每一列
                // 初始化累加和为 0
                var sum = 0f
                // 对所有点进行求和
                for (i in 0 until n) {
                    // 根据行索引选择对应的 A 矩阵上半部分元素
                    val ar = when (r) { 0 -> a11[i]; 1 -> a12[i]; 2 -> a13[i]; else -> a14[i] }
                    // 根据列索引选择对应的 A 矩阵上半部分元素
                    val ac = when (c) { 0 -> a11[i]; 1 -> a12[i]; 2 -> a13[i]; else -> a14[i] }
                    // 累加上半部分的乘积
                    sum += ar * ac
                    // 根据行索引选择对应的 A 矩阵下半部分元素
                    val ar2 = when (r) { 0 -> a21[i]; 1 -> a22[i]; 2 -> a23[i]; else -> a24[i] }
                    // 根据列索引选择对应的 A 矩阵下半部分元素
                    val ac2 = when (c) { 0 -> a21[i]; 1 -> a22[i]; 2 -> a23[i]; else -> a24[i] }
                    // 累加下半部分的乘积
                    sum += ar2 * ac2
                } // 结束求和循环
                // 将累加和存入法方程矩阵
                ata[r][c] = sum
            } // 结束列遍历
            // 初始化右侧向量的累加和为 0
            var sumB = 0f
            // 对所有点进行右侧向量的求和
            for (i in 0 until n) {
                // 根据行索引选择对应的 A 矩阵上半部分元素
                val ar = when (r) { 0 -> a11[i]; 1 -> a12[i]; 2 -> a13[i]; else -> a14[i] }
                // 累加上半部分元素与目标 x 坐标的乘积
                sumB += ar * bx[i]
                // 根据行索引选择对应的 A 矩阵下半部分元素
                val ar2 = when (r) { 0 -> a21[i]; 1 -> a22[i]; 2 -> a23[i]; else -> a24[i] }
                // 累加下半部分元素与目标 y 坐标的乘积
                sumB += ar2 * by[i]
            } // 结束右侧向量求和
            // 将累加和存入右侧向量
            atb[r] = sumB
        } // 结束法方程构建

        // 使用高斯消元法求解 4x4 线性方程组，获取变换参数
        val params = solve4x4(ata, atb) ?: return Matrix() // 求解失败则返回单位矩阵

        // 提取变换参数：缩放/旋转的 x 分量
        val a = params[0]
        // 提取变换参数：缩放/旋转的 y 分量
        val b = params[1]
        // 提取变换参数：x 方向平移量
        val tx = params[2]
        // 提取变换参数：y 方向平移量
        val ty = params[3]

        // 构造 Android Matrix 的 3x3 仿射变换矩阵值数组
        val values = floatArrayOf(a, -b, tx, b, a, ty, 0f, 0f, 1f)
        // 创建 Matrix 对象并设置变换矩阵值，返回相似变换矩阵
        return Matrix().apply { setValues(values) }
    } // 结束 estimateSimilarityTransform 方法

    // 私有方法：使用高斯消元法（列主元）求解 4x4 线性方程组 Ax = b
    private fun solve4x4(A: Array<FloatArray>, b: FloatArray): FloatArray? {
        // 设置矩阵维度为 4
        val n = 4
        // 创建增广矩阵 [A|b]，每行有 5 个元素（4 个系数 + 1 个常数项）
        val aug = Array(n) { FloatArray(n + 1) }
        // 遍历每一行
        for (i in 0 until n) {
            // 将系数矩阵 A 的第 i 行复制到增广矩阵
            for (j in 0 until n) aug[i][j] = A[i][j]
            // 将常数向量 b 的第 i 个元素放到增广矩阵的最后一列
            aug[i][n] = b[i]
        } // 结束增广矩阵构建

        // 列主元高斯消元过程
        for (col in 0 until n) { // 遍历每一列
            // 初始化最大值所在行为当前列
            var maxRow = col
            // 获取当前对角线元素的绝对值作为初始最大值
            var maxVal = kotlin.math.abs(aug[col][col])
            // 在当前列的剩余行中寻找绝对值最大的元素
            for (row in col + 1 until n) {
                // 获取当前行当前列元素的绝对值
                val v = kotlin.math.abs(aug[row][col])
                // 如果比当前最大值更大，更新最大值和所在行
                if (v > maxVal) { maxVal = v; maxRow = row }
            } // 结束列主元搜索
            // 如果最大值接近零，说明矩阵奇异，无法求解
            if (maxVal < 1e-10f) return null

            // 交换当前行与最大值所在行（行交换）
            val tmp = aug[col]; aug[col] = aug[maxRow]; aug[maxRow] = tmp

            // 获取主元值（对角线元素）
            val pivot = aug[col][col]
            // 将当前行除以主元，使对角线元素变为 1
            for (j in col..n) aug[col][j] /= pivot

            // 消元：将当前列其他行的元素变为 0
            for (row in 0 until n) { // 遍历所有行
                // 跳过当前行本身
                if (row == col) continue
                // 获取消元因子（当前行当前列的值）
                val factor = aug[row][col]
                // 从当前行减去消元因子乘以主元行
                for (j in col..n) aug[row][j] -= factor * aug[col][j]
            } // 结束消元循环
        } // 结束高斯消元

        // 回代：增广矩阵最后一列即为解向量
        return FloatArray(n) { i -> aug[i][n] }
    } // 结束 solve4x4 方法

    /**
     * 带边距的人脸裁剪（无关键点时的回退方案）
     */
    // 私有方法：在没有关键点时使用带边距的矩形裁剪作为回退方案
    private fun cropFaceWithMargin(sourceBitmap: Bitmap, rect: Rect): Bitmap {
        // 计算检测框的宽度
        val w = rect.right - rect.left
        // 计算检测框的高度
        val h = rect.bottom - rect.top
        // 计算检测框的中心 x 坐标
        val centerX = rect.left + w / 2f
        // 计算检测框的中心 y 坐标
        val centerY = rect.top + h / 2f
        // 计算裁剪区域大小，取宽高中较大值并扩大 20% 作为边距
        val size = maxOf(w, h) * 1.2f

        // 计算裁剪区域左边界，限制在图像范围内
        val left = (centerX - size / 2).toInt().coerceIn(0, sourceBitmap.width - 1)
        // 计算裁剪区域上边界，限制在图像范围内
        val top = (centerY - size / 2).toInt().coerceIn(0, sourceBitmap.height - 1)
        // 计算裁剪区域右边界，限制在图像范围内
        val right = (centerX + size / 2).toInt().coerceIn(left + 1, sourceBitmap.width)
        // 计算裁剪区域下边界，限制在图像范围内
        val bottom = (centerY + size / 2).toInt().coerceIn(top + 1, sourceBitmap.height)

        // 计算实际裁剪宽度
        val cropW = right - left
        // 计算实际裁剪高度
        val cropH = bottom - top
        // 检查裁剪区域是否有效（宽高大于 0）
        if (cropW <= 0 || cropH <= 0) {
            // 如果裁剪区域无效，直接缩放原图到目标尺寸作为回退
            return Bitmap.createScaledBitmap(sourceBitmap, REC_INPUT_SIZE, REC_INPUT_SIZE, true)
        } // 结束无效裁剪检查

        // 从源图像中裁剪出人脸区域
        val cropped = Bitmap.createBitmap(sourceBitmap, left, top, cropW, cropH)
        // 将裁剪结果缩放到识别模型输入尺寸（112x112）
        val scaled = Bitmap.createScaledBitmap(cropped, REC_INPUT_SIZE, REC_INPUT_SIZE, true)
        // 释放中间裁剪图像的内存
        cropped.recycle()
        // 返回缩放后的人脸图像
        return scaled
    } // 结束 cropFaceWithMargin 方法

    /**
     * 提取人脸特征 - 高性能版本
     * 返回 null 表示模型未加载或推理失败，调用方可区分"失败"与"未匹配"。
     */
    // 挂起函数：在后台线程中提取人脸特征向量（512 维嵌入），失败返回 null
    suspend fun extractEmbedding(faceBitmap: Bitmap): FloatArray? = withContext(Dispatchers.Default) {
        // 检查识别模型是否已加载
        if (recSession == null || env == null) {
            // 打印识别模型未加载的错误日志
            Log.e(TAG, "识别模型未加载")
            // 返回 null 表示模型不可用
            return@withContext null
        } // 结束模型加载检查

        // 声明输入张量变量
        var inputTensor: OnnxTensor? = null
        // 声明输出结果变量
        var outputs: OrtSession.Result? = null

        // 尝试执行特征提取推理
        try {
            // 记录特征提取开始的时间戳
            val startTime = System.currentTimeMillis()

            // 对人脸图像进行预处理（缩放、归一化、转为张量）
            inputTensor = preprocessRecognition(faceBitmap)
            // 运行识别模型推理
            // ⚠️ OrtSession.run 非线程安全，持 inferenceMutex 保护。
            outputs = inferenceMutex.withLock {
                recSession?.run(mapOf(recInputName to inputTensor))
            }

            // 从模型输出中提取原始特征向量，处理不同的输出类型
            val rawEmbedding = when (val outputValue = outputs?.get(0)?.value) {
                // 如果输出是二维数组（批量输出），取第一个样本
                is Array<*> -> (outputValue.getOrNull(0) as? FloatArray)
                    ?: FloatArray(EMBEDDING_DIM)
                // 如果输出直接是一维浮点数组，直接使用
                is FloatArray -> outputValue
                // 其他类型返回零向量（但仍非 null，表示推理成功但输出格式异常）
                else -> FloatArray(EMBEDDING_DIM)
            } // 结束输出类型匹配

            // 对特征向量进行 L2 归一化，使其成为单位向量
            val normalizedEmbedding = l2Normalize(rawEmbedding)

            // 计算特征提取的耗时
            val elapsed = System.currentTimeMillis() - startTime
            // 仅 Debug 构建打印特征提取调试信息
            if (BuildConfig.DEBUG) {
                // 计算原始特征向量的 L2 范数（用于调试）
                val norm = sqrt(rawEmbedding.fold(0f) { acc, v -> acc + v * v })
                // 取归一化后向量的前 5 个值作为采样
                val sample = normalizedEmbedding.take(5).joinToString(", ") { "%.4f".format(it) }
                // 打印特征提取完成的调试日志
                Log.d(TAG, "特征提取完成, 耗时 ${elapsed}ms, dim=${rawEmbedding.size}, norm=${"%.4f".format(norm)}, sample=[$sample]")
            }

            // 返回归一化后的特征向量
            normalizedEmbedding

        // 捕获特征提取过程中的任何异常
        } catch (e: Exception) {
            // 打印特征提取失败的错误日志
            Log.e(TAG, "特征提取失败: ${e.message}", e)
            // 返回 null 表示推理失败，调用方可显式区分错误
            null
        // 无论成功与否都释放资源
        } finally {
            // 安全关闭输入张量
            try { inputTensor?.close() } catch (_: Exception) {}
            // 安全关闭输出结果
            try { outputs?.close() } catch (_: Exception) {}
        } // 结束资源清理
    } // 结束 extractEmbedding 方法

    /**
     * 识别人脸 - 批量比对优化
     */
    // 挂起函数：在后台线程中进行人脸识别，将人脸与模板库比对
    suspend fun recognizeFace(
        faceBitmap: Bitmap, // 待识别人脸图像
        templates: Map<String, FloatArray>, // 已注册人脸的特征模板库（名称 -> 特征向量）
        threshold: Float = 0.3f // 识别阈值，默认 0.3
    ): RecognitionResult = withContext(Dispatchers.Default) {

        // 模板库应由调用方（ViewModel.initialize / setTemplate）在加载时一次性过滤维度，
        // 此处直接信任传入参数，避免每次识别重复 O(N) 过滤 + Map 分配。
        if (templates.isEmpty()) {
            // 打印模板库为空的警告日志
            Log.w(TAG, "识别: 模板库为空")
            // 返回未知识别结果并退出
            return@withContext RecognitionResult("UNKNOWN", 0f)
        } // 结束空模板检查

        // 防御性：仅对维度明显错误的模板跳过（保留容错），不做全量 filter 分配新 Map
        val validTemplates = templates // 直接使用传入的模板库
        // 提取待识别人脸的特征向量
        val embedding = extractEmbedding(faceBitmap)
        // 推理失败时返回 ERROR 结果，UI 可显式提示（区别于"未匹配到模板"的 UNKNOWN）
        if (embedding == null) {
            Log.e(TAG, "识别: 特征提取失败，返回 ERROR 结果")
            return@withContext RecognitionResult("ERROR", -1f)
        } // 结束失败检查

        // 初始化最佳匹配名称为未知
        var bestName = "UNKNOWN"
        // 初始化最高相似度分数为 0
        var bestScore = 0f
        // 初始化第二高相似度分数为 0
        var secondBestScore = 0f

        // 遍历所有有效模板进行比对
        for ((name, templateEmbedding) in validTemplates) {
            // 计算当前人脸与模板之间的余弦相似度
            val similarity = cosineSimilarity(embedding, templateEmbedding)
            // 如果当前相似度高于最高分数
            if (similarity > bestScore) {
                // 将原来的最高分数降为第二高
                secondBestScore = bestScore
                // 更新最高分数
                bestScore = similarity
                // 更新最佳匹配名称
                bestName = name
            // 如果不是最高但高于第二高
            } else if (similarity > secondBestScore) {
                // 更新第二高分数
                secondBestScore = similarity
            } // 结束相似度比较
        } // 结束模板遍历

        // 打印识别结果的调试日志
        Log.d(TAG, "识别结果: best=$bestName score=${"%.4f".format(bestScore)} " +
            "second=${"%.4f".format(secondBestScore)} threshold=$threshold " +
            "validTemplates=${validTemplates.size}")

        // 检查最高分数是否达到识别阈值
        if (bestScore >= threshold) {
            // 分数达到阈值，返回识别成功的结果
            RecognitionResult(bestName, bestScore)
        // 分数未达到阈值
        } else {
            // 返回未知识别结果
            RecognitionResult("UNKNOWN", bestScore)
        } // 结束阈值判断
    } // 结束 recognizeFace 方法

    /**
     * 并行识别多张人脸
     * 对齐(CPU) 和模板匹配(CPU) 完全并行，ONNX 推理通过信号量限制并发数
     */
    suspend fun recognizeFacesParallel(
        sourceBitmap: Bitmap,
        detections: List<FaceDetection>,
        templates: Map<String, FloatArray>,
        threshold: Float = 0.3f,
        maxConcurrency: Int = 2
    ): List<RecognitionResult> = coroutineScope {
        if (detections.isEmpty()) return@coroutineScope emptyList()
        // 单张人脸走原有顺序路径，无并行开销
        if (detections.size == 1) {
            val faceBitmap = alignFace(sourceBitmap, detections[0])
            try {
                return@coroutineScope listOf(recognizeFace(faceBitmap, templates, threshold))
            } finally {
                faceBitmap.recycle()
            }
        }

        // 信号量限制 ONNX 推理并发数（Session.run 非线程安全）
        val semaphore = Semaphore(maxConcurrency)
        val startTime = System.currentTimeMillis()

        // 每张人脸一个 async：对齐完全并行，推理受信号量保护
        val deferreds = detections.mapIndexed { index, detection ->
            async(Dispatchers.Default) {
                var faceBitmap: Bitmap? = null
                try {
                    faceBitmap = alignFace(sourceBitmap, detection)
                    semaphore.withPermit {
                        recognizeFace(faceBitmap, templates, threshold)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "并行识别: 人脸 ${index + 1} 失败: ${e.message}", e)
                    RecognitionResult("UNKNOWN", 0f)
                } finally {
                    faceBitmap?.recycle()
                }
            }
        }

        val results = deferreds.map { it.await() }
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "并行识别完成: ${detections.size} 张人脸, 耗时 ${elapsed}ms, 并发上限=$maxConcurrency")
        results
    }

    // 私有方法：确保 Bitmap 为软件渲染格式（非 HARDWARE），以便读取像素
    private fun ensureSoftware(bitmap: Bitmap): Bitmap {
        // 检查 Bitmap 是否为硬件加速格式
        if (bitmap.config == Bitmap.Config.HARDWARE) {
            // 尝试将 HARDWARE Bitmap 复制为 ARGB_8888 格式
            val sw = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            // 如果复制成功，返回软件格式的 Bitmap
            if (sw != null) return sw
            // 打印格式转换失败的警告
            Log.w(TAG, "HARDWARE→ARGB_8888 失败，尝试 createScaledBitmap")
            // 使用 createScaledBitmap 作为备用方案进行格式转换
            val scaled = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
            // 检查转换后的格式是否不再是 HARDWARE
            if (scaled.config != Bitmap.Config.HARDWARE) return scaled
        } // 结束 HARDWARE 格式检查
        // 如果已经是软件格式，直接返回原 Bitmap
        return bitmap
    } // 结束 ensureSoftware 方法

    /**
     * 预处理检测输入 - InsightFace SCRFD 标准预处理
     * mean=[127.5, 127.5, 127.5], std=[128.0, 128.0, 128.0], BGR 格式
     */
    // 私有方法：对检测输入图像进行预处理，返回 ONNX 张量
    private fun preprocessDetection(bitmap: Bitmap): OnnxTensor {
        // 确保 Bitmap 为软件渲染格式以便读取像素
        val safeBitmap = ensureSoftware(bitmap)
        // 仅 Debug 构建打印预处理信息
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "原始图片: ${bitmap.width}x${bitmap.height} config=${bitmap.config} → safe=${safeBitmap.config}")
        }
        // 将图像缩放到检测模型输入尺寸（640x640）
        val scaledBitmap = Bitmap.createScaledBitmap(safeBitmap, DET_INPUT_SIZE, DET_INPUT_SIZE, true)
        // 如果安全 Bitmap 不是原始 Bitmap，释放中间 Bitmap 的内存
        if (safeBitmap !== bitmap) safeBitmap.recycle()

        // 创建像素缓冲区用于存储缩放后图像的原始像素数据
        val pixelBuffer = IntArray(detPixelBufferSize)
        // 创建浮点缓冲区用于存储预处理后的归一化数据（CHW 格式）
        val floatBuffer = FloatArray(detFloatBufferSize)

        // 尝试执行像素提取和归一化处理
        try {
            // 从缩放后的 Bitmap 中提取所有像素到整数数组
            scaledBitmap.getPixels(pixelBuffer, 0, DET_INPUT_SIZE, 0, 0, DET_INPUT_SIZE, DET_INPUT_SIZE)

            // 计算单通道像素总数（640*640）
            val size = DET_INPUT_SIZE * DET_INPUT_SIZE
            // 遍历所有像素进行 BGR 通道分离和归一化
            for (i in 0 until size) {
                // 获取当前像素的 ARGB 整数值
                val pixel = pixelBuffer[i]
                // 提取 B 通道（最低字节），进行 (pixel - 127.5) / 128.0 归一化
                floatBuffer[i] = ((pixel and 0xFF) - 127.5f) / 128.0f
                // 提取 G 通道（第二字节），进行 (pixel - 127.5) / 128.0 归一化
                floatBuffer[i + size] = (((pixel shr 8) and 0xFF) - 127.5f) / 128.0f
                // 提取 R 通道（第三字节），进行 (pixel - 127.5) / 128.0 归一化
                floatBuffer[i + 2 * size] = (((pixel shr 16) and 0xFF) - 127.5f) / 128.0f
            } // 结束像素归一化循环

            // 仅 Debug 构建打印像素采样和范围，避免 Release 刷屏和 CPU 开销
            if (BuildConfig.DEBUG) {
                // 取前 5 个像素值格式化为十六进制字符串用于调试
                val sample = pixelBuffer.take(5).joinToString(", ") { "0x%08X".format(it) }
                // 打印前 5 个原始像素的十六进制值
                Log.v(TAG, "前5像素: $sample")
                // 打印归一化后浮点数据的范围（最小值和最大值）
                Log.v(TAG, "float范围: [${"%.4f".format(floatBuffer.minOrNull())}, ${"%.4f".format(floatBuffer.maxOrNull())}]")
            }

            // 定义张量的形状：[batch=1, channels=3, height=640, width=640]（NCHW 格式）
            val shape = longArrayOf(1, 3, DET_INPUT_SIZE.toLong(), DET_INPUT_SIZE.toLong())
            // 使用 FloatBuffer 包装浮点数组并创建 ONNX 张量
            return OnnxTensor.createTensor(env, FloatBuffer.wrap(floatBuffer), shape)
        // 无论成功与否都释放缩放后的 Bitmap
        } finally {
            // 释放缩放后 Bitmap 的内存
            scaledBitmap.recycle()
        } // 结束资源清理
    } // 结束 preprocessDetection 方法

    /**
     * 预处理识别输入 - InsightFace ArcFace 标准预处理
     * mean=[127.5, 127.5, 127.5], std=[128.0, 128.0, 128.0], BGR 格式
     */
    // 私有方法：对识别输入人脸图像进行预处理，返回 ONNX 张量
    private fun preprocessRecognition(bitmap: Bitmap): OnnxTensor {
        // 确保 Bitmap 为软件渲染格式以便读取像素
        val safeBitmap = ensureSoftware(bitmap)
        // 将图像缩放到识别模型输入尺寸（112x112）
        val scaledBitmap = Bitmap.createScaledBitmap(safeBitmap, REC_INPUT_SIZE, REC_INPUT_SIZE, true)
        // 如果安全 Bitmap 不是原始 Bitmap，释放中间 Bitmap 的内存
        if (safeBitmap !== bitmap) safeBitmap.recycle()

        // 创建像素缓冲区用于存储缩放后图像的原始像素数据
        val pixelBuffer = IntArray(recPixelBufferSize)
        // 创建浮点缓冲区用于存储预处理后的归一化数据（CHW 格式）
        val floatBuffer = FloatArray(recFloatBufferSize)

        // 尝试执行像素提取和归一化处理
        try {
            // 从缩放后的 Bitmap 中提取所有像素到整数数组
            scaledBitmap.getPixels(pixelBuffer, 0, REC_INPUT_SIZE, 0, 0, REC_INPUT_SIZE, REC_INPUT_SIZE)

            // 计算单通道像素总数（112*112）
            val size = REC_INPUT_SIZE * REC_INPUT_SIZE
            // 遍历所有像素进行 BGR 通道分离和归一化
            for (i in 0 until size) {
                // 获取当前像素的 ARGB 整数值
                val pixel = pixelBuffer[i]
                // 提取 B 通道（最低字节），进行 (pixel - 127.5) / 128.0 归一化
                floatBuffer[i] = ((pixel and 0xFF) - 127.5f) / 128.0f
                // 提取 G 通道（第二字节），进行 (pixel - 127.5) / 128.0 归一化
                floatBuffer[i + size] = (((pixel shr 8) and 0xFF) - 127.5f) / 128.0f
                // 提取 R 通道（第三字节），进行 (pixel - 127.5) / 128.0 归一化
                floatBuffer[i + 2 * size] = (((pixel shr 16) and 0xFF) - 127.5f) / 128.0f
            } // 结束像素归一化循环

            // 定义张量的形状：[batch=1, channels=3, height=112, width=112]（NCHW 格式）
            val shape = longArrayOf(1, 3, REC_INPUT_SIZE.toLong(), REC_INPUT_SIZE.toLong())
            // 使用 FloatBuffer 包装浮点数组并创建 ONNX 张量
            return OnnxTensor.createTensor(env, FloatBuffer.wrap(floatBuffer), shape)
        // 无论成功与否都释放缩放后的 Bitmap
        } finally {
            // 释放缩放后 Bitmap 的内存
            scaledBitmap.recycle()
        } // 结束资源清理
    } // 结束 preprocessRecognition 方法

    /**
     * 解析检测输出 - 支持 InsightFace 的 det_10g 输出格式
     * 格式: [1, N, 15] = [x1, y1, x2, y2, score, kp0_x, kp0_y, ..., kp4_y]
     */
    // 私有方法：解析单输出格式的检测模型结果
    private fun parseDetectionOutputs(
        outputs: OrtSession.Result?, // 模型输出结果
        imgWidth: Int, // 原始图像宽度
        imgHeight: Int, // 原始图像高度
        detectionThreshold: Float = DET_CONF_THRESHOLD // 检测置信度阈值
    ): List<FaceDetection> {
        // 创建可变列表用于存储检测结果
        val detections = mutableListOf<FaceDetection>()
        // 计算 x 方向的缩放比例（原始图像 / 模型输入）
        val scaleX = imgWidth.toFloat() / DET_INPUT_SIZE
        // 计算 y 方向的缩放比例（原始图像 / 模型输入）
        val scaleY = imgHeight.toFloat() / DET_INPUT_SIZE

        // 如果输出为 null，直接返回空列表
        if (outputs == null) return detections

        // 尝试解析输出数据
        try {
            // 获取第一个输出张量
            val outputTensor = outputs.get(0)
            // 获取输出张量的原始值
            val rawValue = outputTensor.value

            // 打印检测输出的数据类型
            Log.d(TAG, "检测输出类型: ${rawValue?.javaClass?.name}")

            // 将原始输出值展平为一维浮点数组
            val flatDetections = extractDetectionFloats(rawValue)

            // 如果未能提取到任何数据
            if (flatDetections.isEmpty()) {
                // 打印未能提取数据的警告
                Log.w(TAG, "未能从检测输出提取到任何数据")
                // 返回空列表
                return detections
            } // 结束空数据检查
            // 根据数据总数推断每行的步长（15=完整检测, 6=bbox+score, 5=bbox）
            val stride = when {
                // 如果总数能被 15 整除，说明是完整格式（bbox+score+5关键点）
                flatDetections.size % 15 == 0 -> 15
                // 如果总数能被 6 整除，说明是 bbox+score 格式
                flatDetections.size % 6 == 0 -> 6
                // 如果总数能被 5 整除，说明是 bbox 格式
                flatDetections.size % 5 == 0 -> 5
                // 无法确定步长
                else -> 0
            } // 结束步长推断

            // 如果无法确定步长
            if (stride == 0) {
                // 打印无法确定步长的警告
                Log.w(TAG, "无法确定检测输出步长: 共 ${flatDetections.size} 个值")
                // 返回空列表
                return detections
            } // 结束步长验证

            // 计算检测框的数量
            val count = flatDetections.size / stride
            // 打印检测输出的行数和步长
            Log.d(TAG, "检测输出: $count 行, 步长 $stride")

            // 取前 5 个检测框的置信度分数作为采样
            val sampleScores = (0 until min(count, 5)).map { i -> flatDetections[i * stride + 4] }
            // 打印置信度采样和阈值
            Log.d(TAG, "前5个检测框置信度采样: $sampleScores (阈值: $detectionThreshold)")

            // 遍历所有检测框
            for (i in 0 until count) {
                // 计算当前检测框在扁平数组中的起始偏移量
                val offset = i * stride
                // 获取原始置信度分数
                var score = flatDetections[offset + 4]
                // 如果分数不在 [0, 1] 范围内，使用 sigmoid 函数进行转换
                if (score < 0f || score > 1f) {
                    // 应用 sigmoid 函数: 1 / (1 + e^(-x))
                    score = (1.0 / (1.0 + Math.exp(-score.toDouble()))).toFloat()
                } // 结束 sigmoid 转换
                // 如果置信度低于阈值，跳过此检测框
                if (score <= detectionThreshold) continue

                // 提取并缩放边界框左上角 x 坐标，限制在图像范围内
                val x1 = (flatDetections[offset + 0] * scaleX).toInt().coerceIn(0, imgWidth)
                // 提取并缩放边界框左上角 y 坐标，限制在图像范围内
                val y1 = (flatDetections[offset + 1] * scaleY).toInt().coerceIn(0, imgHeight)
                // 提取并缩放边界框右下角 x 坐标，限制在图像范围内
                val x2 = (flatDetections[offset + 2] * scaleX).toInt().coerceIn(0, imgWidth)
                // 提取并缩放边界框右下角 y 坐标，限制在图像范围内
                val y2 = (flatDetections[offset + 3] * scaleY).toInt().coerceIn(0, imgHeight)

                // 检查边界框是否有效（右下角大于左上角）
                if (x2 <= x1 || y2 <= y1) continue
                // 计算人脸框的宽度
                val faceW = x2 - x1
                // 计算人脸框的高度
                val faceH = y2 - y1
                // 过滤太小的检测框（宽或高小于 20 像素）
                if (faceW < 20 || faceH < 20) continue
                // 计算人脸框的宽高比
                val aspectRatio = faceH.toFloat() / faceW.toFloat()
                // 过滤宽高比异常的检测框（太扁或太窄）
                if (aspectRatio < 0.3f || aspectRatio > 4.0f) continue

                // 创建关键点列表
                val landmarks = mutableListOf<Pair<Float, Float>>()
                // 如果步长 >= 15，说明包含 5 个关键点数据
                if (stride >= 15) {
                    // 遍历 5 个关键点（左眼、右眼、鼻尖、左嘴角、右嘴角）
                    for (k in 0 until 5) {
                        // 添加关键点坐标（x, y），并进行缩放映射到原图坐标
                        landmarks.add(
                            Pair(
                                flatDetections[offset + 5 + k * 2] * scaleX, // 关键点 x 坐标
                                flatDetections[offset + 6 + k * 2] * scaleY  // 关键点 y 坐标
                            )
                        ) // 结束关键点添加
                    } // 结束关键点遍历
                } // 结束关键点提取

                // 将有效的检测结果添加到列表中
                detections.add(
                    FaceDetection(
                        rect = Rect(x1, y1, x2, y2), // 人脸边界框
                        confidence = score, // 置信度分数
                        landmarks = landmarks // 关键点列表
                    )
                ) // 结束检测结果添加
            } // 结束检测框遍历

            // 打印原始检测数量和通过阈值的数量
            Log.d(TAG, "原始检测: ${count} 行, 通过阈值: ${detections.size}")

        // 捕获解析过程中的任何异常
        } catch (e: Exception) {
            // 打印解析失败的错误日志
            Log.e(TAG, "解析检测输出失败: ${e.javaClass.simpleName} - ${e.message}", e)
        } // 结束异常捕获

        // 返回所有有效的检测结果
        return detections
    } // 结束 parseDetectionOutputs 方法

    /**
     * 解析多步长分离输出 - 使用模型元数据按 lastDim 分类
     * lastDim=1 → score, lastDim=4 → bbox, lastDim=10 → kps
     * anchorCount → 估算 stride
     */
    // 私有方法：解析 SCRFD 多步长分离输出格式（多个输出张量）
    private fun parseMultiStrideOutputs(
        outputs: OrtSession.Result, // 模型输出结果
        imgWidth: Int, // 原始图像宽度
        imgHeight: Int, // 原始图像高度
        detectionThreshold: Float = DET_CONF_THRESHOLD // 检测置信度阈值
    ): List<FaceDetection> {
        // 定义步长分组数据类，记录每个步长对应的输出索引
        data class StrideGroup(
            val stride: Int, // 特征图步长（8, 16, 32 等）
            val scoreIdx: Int = -1, // 置信度输出的索引（-1 表示未找到）
            val bboxIdx: Int = -1, // 边界框输出的索引（-1 表示未找到）
            val kpsIdx: Int = -1 // 关键点输出的索引（-1 表示未找到）
        ) // 结束 StrideGroup 数据类

        // 创建可变列表用于存储检测结果
        val detections = mutableListOf<FaceDetection>()
        // 获取模型输出的张量总数
        val outputSize = outputs.size()

        // 尝试解析多输出数据
        try {
            // 存储置信度输出的索引和锚点数量
            val scoreOutputs = mutableListOf<Pair<Int, Int>>()
            // 存储边界框输出的索引和锚点数量
            val bboxOutputs = mutableListOf<Pair<Int, Int>>()
            // 存储关键点输出的索引和锚点数量
            val kpsOutputs = mutableListOf<Pair<Int, Int>>()

            // 检查缓存的形状信息是否与输出数量匹配
            if (detOutputShapes.size == outputSize) {
                // 使用缓存的形状信息进行分类
                for ((i, shapeInfo) in detOutputShapes.withIndex()) {
                    // 打印每个输出的形状信息
                    Log.d(TAG, "输出[$i] '${shapeInfo.name}': anchorCount=${shapeInfo.anchorCount}, lastDim=${shapeInfo.lastDim}")
                    // 根据最后一维的大小分类输出类型
                    when (shapeInfo.lastDim) {
                        // lastDim=1 表示置信度输出
                        1 -> scoreOutputs.add(i to shapeInfo.anchorCount)
                        // lastDim=4 表示边界框输出
                        4 -> bboxOutputs.add(i to shapeInfo.anchorCount)
                        // lastDim=10 表示 5 个关键点输出（每个点 x,y 共 10 个值）
                        10 -> kpsOutputs.add(i to shapeInfo.anchorCount)
                        // 未知的 lastDim，打印警告
                        else -> Log.w(TAG, "输出[$i]: 未知 lastDim=${shapeInfo.lastDim}")
                    } // 结束 lastDim 分类
                } // 结束形状信息遍历
            // 如果形状信息数量不匹配，使用 flatSize 作为回退分类策略
            } else {
                // 打印回退分类的警告
                Log.w(TAG, "缓存形状数量(${detOutputShapes.size}) != 输出数量($outputSize)，回退 flatSize 分类")
                // 遍历所有输出
                for (i in 0 until outputSize) {
                    // 将输出展平为一维数组
                    val flatArr = extractFlatFloatArray(outputs.get(i).value)
                    // 获取展平后的数组大小
                    val flatSize = flatArr.size
                    // 根据 flatSize 匹配已知的 SCRFD 输出尺寸
                    when {
                        // 匹配置信度输出的常见尺寸
                        flatSize in intArrayOf(12800, 3200, 800, 200, 50) -> scoreOutputs.add(i to flatSize)
                        // 匹配边界框输出的常见尺寸（每个框 4 个值）
                        flatSize in intArrayOf(51200, 12800, 3200, 800, 200) -> bboxOutputs.add(i to flatSize / 4)
                        // 匹配关键点输出的常见尺寸（每个框 10 个值）
                        flatSize in intArrayOf(128000, 32000, 8000, 2000, 500) -> kpsOutputs.add(i to flatSize / 10)
                        // 未匹配的尺寸，打印调试日志
                        else -> Log.d(TAG, "输出[$i]: flatSize=$flatSize 未匹配")
                    } // 结束 flatSize 分类
                } // 结束输出遍历
            } // 结束分类策略选择

            // 打印输出分类结果的汇总
            Log.i(TAG, "输出分类: score=${scoreOutputs.size}, bbox=${bboxOutputs.size}, kps=${kpsOutputs.size}")

            // 创建步长到分组的映射表
            val strideMap = mutableMapOf<Int, StrideGroup>()

            // 将置信度输出按步长分组
            for ((idx, anchorCount) in scoreOutputs) {
                // 估算当前输出对应的特征图步长
                val stride = estimateStride(anchorCount)
                // 获取或创建该步长的分组
                val existing = strideMap.getOrDefault(stride, StrideGroup(stride))
                // 更新分组的置信度输出索引
                strideMap[stride] = existing.copy(scoreIdx = idx)
            } // 结束置信度输出分组

            // 将边界框输出按步长分组
            for ((idx, anchorCount) in bboxOutputs) {
                // 估算当前输出对应的特征图步长
                val stride = estimateStride(anchorCount)
                // 获取或创建该步长的分组
                val existing = strideMap.getOrDefault(stride, StrideGroup(stride))
                // 更新分组的边界框输出索引
                strideMap[stride] = existing.copy(bboxIdx = idx)
            } // 结束边界框输出分组

            // 将关键点输出按步长分组
            for ((idx, anchorCount) in kpsOutputs) {
                // 估算当前输出对应的特征图步长
                val stride = estimateStride(anchorCount)
                // 获取或创建该步长的分组
                val existing = strideMap.getOrDefault(stride, StrideGroup(stride))
                // 更新分组的关键点输出索引
                strideMap[stride] = existing.copy(kpsIdx = idx)
            } // 结束关键点输出分组

            // 按步长排序所有分组
            val groups = strideMap.values.sortedBy { it.stride }
            // 遍历所有分组并打印配置信息
            for (g in groups) {
                // 打印每个步长分组的输出索引配置
                Log.i(TAG, "步长${g.stride}: score=[${g.scoreIdx}], bbox=[${g.bboxIdx}], kps=[${if (g.kpsIdx >= 0) "${g.kpsIdx}" else "无"}]")
            } // 结束分组信息打印

            // 计算 x 方向的缩放比例
            val scaleX = imgWidth.toFloat() / DET_INPUT_SIZE.toFloat()
            // 计算 y 方向的缩放比例
            val scaleY = imgHeight.toFloat() / DET_INPUT_SIZE.toFloat()

            // 遍历每个步长分组进行检测框解码
            for (group in groups) {
                // 检查分组是否缺少必要的输出
                if (group.scoreIdx < 0 || group.bboxIdx < 0) {
                    // 打印缺少输出的警告并跳过
                    Log.w(TAG, "步长${group.stride}: 缺少 score 或 bbox 输出，跳过")
                    // 跳过不完整的分组
                    continue
                } // 结束完整性检查

                // 提取置信度输出的浮点数组
                val scoreValues = extractFlatFloatArray(outputs.get(group.scoreIdx).value)
                // 提取边界框输出的浮点数组
                val bboxValues = extractFlatFloatArray(outputs.get(group.bboxIdx).value)
                // 计算锚点总数
                val numAnchors = scoreValues.size
                // 计算每个边界框的维度（通常为 4：left, top, right, bottom）
                val bboxDim = if (numAnchors > 0) bboxValues.size / numAnchors else 0

                // 打印当前步长的调试信息
                Log.d(TAG, "步长${group.stride}: anchors=$numAnchors, bboxDim=$bboxDim, " +
                    "scoreRange=[${"%.4f".format(scoreValues.minOrNull())}, ${"%.4f".format(scoreValues.maxOrNull())}]")

                // 检查数据有效性
                if (numAnchors == 0 || bboxDim < 4) {
                    // 打印数据无效的警告并跳过
                    Log.w(TAG, "步长${group.stride}: 数据无效，跳过")
                    // 跳过无效数据
                    continue
                } // 结束数据有效性检查

                // 获取当前步长值
                val stride = group.stride
                // 计算特征图的空间尺寸（输入尺寸 / 步长）
                val featSize = DET_INPUT_SIZE / stride
                // 计算每个特征图位置的锚点数量
                val anchorsPerPos = numAnchors / (featSize * featSize)
                // 打印每个位置的锚点数量
                Log.d(TAG, "步长${group.stride}: anchorsPerPos=$anchorsPerPos")

                // 如果有关键点输出，提取关键点数据
                val kpsValues = if (group.kpsIdx >= 0) {
                    // 提取关键点输出的浮点数组
                    extractFlatFloatArray(outputs.get(group.kpsIdx).value)
                // 否则设为 null
                } else null
                // 计算每个检测框对应的关键点值数量
                val kpsPerBox = if (kpsValues != null && numAnchors > 0) kpsValues.size / numAnchors else 0

                // 记录当前分组通过阈值的检测数量
                var groupPassCount = 0
                // 遍历所有锚点进行解码
                for (idx in 0 until numAnchors) {
                    // 获取当前锚点的原始置信度分数
                    var score = scoreValues[idx]
                    // 如果分数不在 [0, 1] 范围内，使用 sigmoid 转换
                    if (score < 0f || score > 1f) {
                        // 应用 sigmoid 函数
                        score = (1.0 / (1.0 + Math.exp(-score.toDouble()))).toFloat()
                    } // 结束 sigmoid 转换
                    // 如果置信度低于阈值，跳过此锚点
                    if (score <= detectionThreshold) continue

                    // 计算当前锚点在特征图上的位置索引
                    val posIdx = idx / anchorsPerPos
                    // 计算特征图上的 y 坐标（行号）
                    val gy = posIdx / featSize
                    // 计算特征图上的 x 坐标（列号）
                    val gx = posIdx % featSize
                    // 计算锚点中心在模型输入图像上的 x 坐标
                    val cx = (gx + 0.5f) * stride
                    // 计算锚点中心在模型输入图像上的 y 坐标
                    val cy = (gy + 0.5f) * stride

                    // 计算当前锚点边界框数据在数组中的起始偏移
                    val bOff = idx * bboxDim
                    // 获取原始左边界偏移量
                    val rawLeft = bboxValues[bOff + 0]
                    // 获取原始上边界偏移量
                    val rawTop = bboxValues[bOff + 1]
                    // 获取原始右边界偏移量
                    val rawRight = bboxValues[bOff + 2]
                    // 获取原始下边界偏移量
                    val rawBottom = bboxValues[bOff + 3]

                    // 判断偏移量是否在合理范围内（用于确定是否需要乘以步长）
                    val useStrideScale = rawLeft in -10f..10f && rawRight in -10f..10f &&
                        rawTop in -10f..10f && rawBottom in -10f..10f
                    // 根据偏移量范围决定缩放因子
                    val s = if (useStrideScale) stride.toFloat() else 1f

                    // 计算边界框左上角 x 坐标并映射到原图
                    val x1 = ((cx - rawLeft * s) * scaleX).toInt().coerceIn(0, imgWidth)
                    // 计算边界框左上角 y 坐标并映射到原图
                    val y1 = ((cy - rawTop * s) * scaleY).toInt().coerceIn(0, imgHeight)
                    // 计算边界框右下角 x 坐标并映射到原图
                    val x2 = ((cx + rawRight * s) * scaleX).toInt().coerceIn(0, imgWidth)
                    // 计算边界框右下角 y 坐标并映射到原图
                    val y2 = ((cy + rawBottom * s) * scaleY).toInt().coerceIn(0, imgHeight)

                    // 检查边界框是否有效（右下角大于左上角）
                    if (x2 <= x1 || y2 <= y1) continue

                    // 计算人脸框的宽度
                    val faceW = x2 - x1
                    // 计算人脸框的高度
                    val faceH = y2 - y1
                    // 过滤太小的检测框
                    if (faceW < 20 || faceH < 20) continue
                    // 计算宽高比
                    val aspectRatio = faceH.toFloat() / faceW.toFloat()
                    // 过滤宽高比异常的检测框
                    if (aspectRatio < 0.3f || aspectRatio > 4.0f) continue

                    // 创建关键点列表
                    val landmarks = mutableListOf<Pair<Float, Float>>()
                    // 如果有关键点数据且每个框有 10 个关键点值
                    if (kpsValues != null && kpsPerBox >= 10) {
                        // 关键点是否也需要乘以步长缩放
                        val kpsStride = if (useStrideScale) stride.toFloat() else 1f
                        // 遍历 5 个关键点
                        for (k in 0 until 5) {
                            // 计算关键点 x 坐标并映射到原图
                            val kx = (cx + kpsValues[idx * kpsPerBox + k * 2] * kpsStride) * scaleX
                            // 计算关键点 y 坐标并映射到原图
                            val ky = (cy + kpsValues[idx * kpsPerBox + k * 2 + 1] * kpsStride) * scaleY
                            // 添加关键点到列表
                            landmarks.add(Pair(kx, ky))
                        } // 结束关键点遍历
                    } // 结束关键点提取

                    // 将解码后的检测结果添加到列表
                    detections.add(
                        FaceDetection(
                            rect = Rect(x1, y1, x2, y2), // 人脸边界框
                            confidence = score.coerceIn(0f, 1f), // 置信度（限制在 [0,1] 范围内）
                            landmarks = landmarks // 关键点列表
                        )
                    ) // 结束检测结果添加
                    // 递增通过阈值的计数
                    groupPassCount++
                } // 结束锚点遍历
                // 打印当前步长的通过率
                Log.d(TAG, "步长$stride: 通过阈值 $groupPassCount / $numAnchors")
            } // 结束分组遍历

            // 打印多输出解析完成的日志
            Log.i(TAG, "多输出解析完成: ${detections.size} 张人脸")

        // 捕获解析过程中的任何异常
        } catch (e: Exception) {
            // 打印多输出解析失败的错误日志
            Log.e(TAG, "多输出解析失败: ${e.javaClass.simpleName} - ${e.message}", e)
        } // 结束异常捕获

        // 返回所有有效的检测结果
        return detections
    } // 结束 parseMultiStrideOutputs 方法

    // 私有方法：根据锚点数量估算特征图的步长
    private fun estimateStride(anchorCount: Int): Int {
        // 计算特征图的空间尺寸（假设特征图为正方形）
        val featSize = kotlin.math.sqrt(anchorCount.toDouble()).toInt()
        // 定义 SCRFD 已知的特征图尺寸
        val knownSizes = intArrayOf(160, 80, 40, 20, 10)
        // 找到最接近已知尺寸的值
        val best = knownSizes.minByOrNull { kotlin.math.abs(it - featSize) } ?: featSize
        // 返回步长（输入尺寸 / 特征图尺寸）
        return DET_INPUT_SIZE / best
    } // 结束 estimateStride 方法

    // 私有方法：获取张量值的采样字符串（用于调试日志）
    private fun getTensorSample(value: Any?): String {
        // 如果值为 null，返回 "null"
        if (value == null) return "null"
        // 尝试提取浮点数组并格式化
        return try {
            // 将值展平为浮点数组
            val floats = extractFlatFloatArray(value)
            // 取前 8 个值作为采样
            val n = minOf(8, floats.size)
            // 格式化采样值为逗号分隔的字符串
            floats.take(n).joinToString(", ") { "%.4f".format(it) } +
                // 如果数组长度大于采样数，追加总数信息
                if (floats.size > n) " ... (共${floats.size})" else ""
        // 捕获异常返回 "?"
        } catch (_: Exception) { "?" }
    } // 结束 getTensorSample 方法

    // 私有方法：将 ONNX 输出值展平为一维浮点数组
    private fun extractFlatFloatArray(value: Any?): FloatArray {
        // 如果值为 null，返回空数组
        if (value == null) return FloatArray(0)
        // 创建可变列表用于收集浮点值
        val result = mutableListOf<Float>()
        // 递归展平所有嵌套结构
        flattenToFloats(value, result)
        // 将列表转换为数组并返回
        return result.toFloatArray()
    } // 结束 extractFlatFloatArray 方法

    // 私有方法：递归地将各种类型的值展平为浮点数列表
    private fun flattenToFloats(value: Any, result: MutableList<Float>) {
        // 根据值的类型进行不同的处理
        when (value) {
            // 如果是浮点数组，直接添加所有元素
            is FloatArray -> result.addAll(value.asList())
            // 如果是单个浮点数，添加到列表
            is Float -> result.add(value)
            // 如果是双精度数，转换为浮点数后添加
            is Double -> result.add(value.toFloat())
            // 如果是双精度数组，逐个转换为浮点数后添加
            is DoubleArray -> { for (d in value) result.add(d.toFloat()) }
            // 如果是整数数组，逐个转换为浮点数后添加
            is IntArray -> { for (i in value) result.add(i.toFloat()) }
            // 如果是长整型数组，逐个转换为浮点数后添加
            is LongArray -> { for (l in value) result.add(l.toFloat()) }
            // 如果是短整型数组，逐个转换为浮点数后添加
            is ShortArray -> { for (s in value) result.add(s.toFloat()) }
            // 如果是其他数值类型，转换为浮点数后添加
            is Number -> result.add(value.toFloat())
            // 如果是数组类型，递归处理每个元素
            is Array<*> -> {
                // 遍历数组中的每个元素
                for (element in value) {
                    // 如果元素不为 null，递归展平
                    if (element != null) flattenToFloats(element, result)
                } // 结束数组遍历
            // 如果是 Java FloatBuffer，读取所有浮点值
            } is java.nio.FloatBuffer -> {
                // 创建数组存储缓冲区中的数据
                val buf = FloatArray(value.remaining())
                // 从缓冲区读取数据到数组
                value.get(buf)
                // 将数组所有元素添加到结果列表
                result.addAll(buf.asList())
            // 如果是 Java DoubleBuffer，读取所有值并转换
            } is java.nio.DoubleBuffer -> {
                // 创建数组存储缓冲区中的数据
                val buf = DoubleArray(value.remaining())
                // 从缓冲区读取数据到数组
                value.get(buf)
                // 逐个转换为浮点数并添加
                for (d in buf) result.add(d.toFloat())
            // 其他类型忽略
            } else -> {}
        } // 结束类型匹配
    } // 结束 flattenToFloats 方法

    /**
     * 从 ONNX tensor value 提取 float 数组，处理各种可能的返回类型
     */
    // 私有方法：从检测输出的原始值中提取浮点数组
    private fun extractDetectionFloats(rawValue: Any?): FloatArray {
        // 如果原始值为 null，返回空数组
        if (rawValue == null) return FloatArray(0)
        // 委托给通用的展平方法处理
        return extractFlatFloatArray(rawValue)
    } // 结束 extractDetectionFloats 方法

    /**
     * 诊断扫描 - 列出所有输出中的 Top-10 最高分数
     * 用于排查模型是否在输出、分数是否在阈值附近
     */
    // 私有方法：诊断扫描所有输出，找出最高分数用于排查问题
    private fun diagnosticScanOutputs(outputs: OrtSession.Result, outputCount: Int) {
        // 定义分数条目数据类，存储分数、原始值、输出索引和锚点索引
        data class ScoreEntry(val score: Float, val rawScore: Float, val outputIdx: Int, val anchorIdx: Int)

        // 创建列表收集所有有效的分数
        val allScores = mutableListOf<ScoreEntry>()

        // 遍历所有输出张量
        for (i in 0 until outputCount) {
            // 尝试读取当前输出
            try {
                // 将输出展平为浮点数组
                val flatArr = extractFlatFloatArray(outputs.get(i).value)
                // 如果数组为空，跳过
                if (flatArr.isEmpty()) continue

                // 判断是否为分数输出（通过数组大小粗略判断）
                val isScoreOutput = flatArr.size < 50000

                // 如果是分数输出
                if (isScoreOutput) {
                    // 遍历所有值
                    for (idx in flatArr.indices) {
                        // 获取原始值
                        var raw = flatArr[idx]
                        // 跳过 NaN 和无穷大的值
                        if (raw.isNaN() || raw.isInfinite()) continue
                        // 初始化概率值
                        var prob = raw
                        // 如果概率不在 [0, 1] 范围内，使用 sigmoid 转换
                        if (prob < 0f || prob > 1f) {
                            // 应用 sigmoid 函数
                            prob = (1.0 / (1.0 + Math.exp(-raw.toDouble()))).toFloat()
                        } // 结束 sigmoid 转换
                        // 将分数条目添加到列表
                        allScores.add(ScoreEntry(prob, raw, i, idx))
                    } // 结束值遍历
                // 如果不是分数输出，跳过
                }
            // 捕获读取异常并忽略
            } catch (_: Exception) {}
        } // 结束输出遍历

        // 按分数降序排序，取前 10 个最高分数
        val top10 = allScores.sortedByDescending { it.score }.take(10)
        // 检查是否有有效分数
        if (top10.isEmpty()) {
            // 如果没有任何有效分数，打印严重错误
            Log.e(TAG, "诊断: 没有任何有效的分数数据！模型可能没有正确运行")
        // 如果有有效分数
        } else {
            // 打印 Top-10 最高分数的标题
            Log.w(TAG, "诊断: Top-10 最高分数:")
            // 遍历并打印每个高分条目
            for ((rank, entry) in top10.withIndex()) {
                // 打印排名、概率、原始分数、输出索引和锚点索引
                Log.w(TAG, "  #${rank + 1}: prob=${"%.6f".format(entry.score)}, raw=${"%.4f".format(entry.rawScore)}, output=${entry.outputIdx}, anchor=${entry.anchorIdx}")
            } // 结束高分打印
            // 获取最高分数
            val maxScore = top10[0].score
            // 如果最高分数极低（< 0.01），说明模型推理可能异常
            if (maxScore < 0.01f) {
                // 打印模型推理结果异常的错误
                Log.e(TAG, "诊断: 最高分数 < 0.01 → 模型推理结果异常，输入可能无效或模型不兼容")
            // 如果最高分数低于检测阈值
            } else if (maxScore < DET_CONF_THRESHOLD) {
                // 打印阈值过高的警告，建议降低阈值
                Log.w(TAG, "诊断: 最高分数 ${"%.4f".format(maxScore)} < 阈值 $DET_CONF_THRESHOLD → 检测阈值过高，将尝试临时降低阈值重新检测")
            } // 结束分数诊断
        } // 结束空分数处理
    } // 结束 diagnosticScanOutputs 方法

    /**
     * NMS (非极大值抑制)
     */
    // 私有方法：对检测结果应用非极大值抑制，去除重叠的检测框
    private fun applyNMS(detections: List<FaceDetection>, iouThreshold: Float): List<FaceDetection> {
        // 如果检测结果不超过 1 个，直接返回
        if (detections.size <= 1) return detections

        // 按置信度降序排序检测结果
        val sorted = detections.sortedByDescending { it.confidence }
        // 创建保留列表存储通过 NMS 的检测结果
        val keep = mutableListOf<FaceDetection>()
        // 创建布尔数组标记被抑制的检测框
        val suppressed = BooleanArray(sorted.size)

        // 遍历所有检测框
        for (i in sorted.indices) {
            // 如果当前框已被抑制，跳过
            if (suppressed[i]) continue

            // 将当前框添加到保留列表
            keep.add(sorted[i])

            // 遍历当前框之后的所有框
            for (j in i + 1 until sorted.size) {
                // 如果该框已被抑制，跳过
                if (suppressed[j]) continue
                // 如果两个框的 IoU 超过阈值，抑制后者
                if (calculateIoU(sorted[i].rect, sorted[j].rect) > iouThreshold) {
                    // 标记该框为已抑制
                    suppressed[j] = true
                } // 结束 IoU 判断
            } // 结束后续框遍历
        } // 结束所有框遍历

        // 返回通过 NMS 的检测结果
        return keep
    } // 结束 applyNMS 方法

    /**
     * 计算两个矩形 IoU
     */
    // 私有方法：计算两个矩形的交并比（Intersection over Union）
    private fun calculateIoU(a: Rect, b: Rect): Float {
        // 计算交集区域的左边界（取两个矩形左边界的最大值）
        val x1 = max(a.left, b.left)
        // 计算交集区域的上边界（取两个矩形上边界的最大值）
        val y1 = max(a.top, b.top)
        // 计算交集区域的右边界（取两个矩形右边界的最小值）
        val x2 = min(a.right, b.right)
        // 计算交集区域的下边界（取两个矩形下边界的最小值）
        val y2 = min(a.bottom, b.bottom)

        // 计算交集面积（如果无交集则为 0）
        val intersection = max(0, x2 - x1) * max(0, y2 - y1)
        // 计算矩形 A 的面积
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        // 计算矩形 B 的面积
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        // 计算并集面积 = 面积A + 面积B - 交集面积
        val union = areaA + areaB - intersection

        // 返回交并比（交集 / 并集），如果并集为 0 则返回 0
        return if (union > 0) intersection.toFloat() / union else 0f
    } // 结束 calculateIoU 方法

    /**
     * L2 归一化 - 原地计算减少内存分配
     */
    // 私有方法：对特征向量进行 L2 归一化，使其成为单位向量
    private fun l2Normalize(vector: FloatArray): FloatArray {
        // 初始化平方和为 0
        var sum = 0f
        // 遍历向量中所有元素，累加平方值
        for (v in vector) {
            // 累加当前元素的平方
            sum += v * v
        } // 结束平方和计算
        // 计算 L2 范数（平方和的平方根）
        val norm = sqrt(sum)
        // 如果范数大于 0（非零向量）
        if (norm > 0) {
            // 遍历向量中所有元素
            for (i in vector.indices) {
                // 将每个元素除以范数，实现归一化
                vector[i] /= norm
            } // 结束归一化循环
        } // 结束范数检查
        // 返回归一化后的向量（原地修改）
        return vector
    } // 结束 l2Normalize 方法

    /**
     * 余弦相似度 - 两侧向量均已 L2 归一化
     * 归一化后范数=1，余弦相似度 = 点积，省去两次 sqrt 和除法。
     * 若向量意外未归一化（norm≈0），点积仍返回 0，行为安全。
     */
    // 私有方法：计算两个已归一化向量之间的余弦相似度（= 点积）
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        // 校验维度一致，不一致直接返回 0（防御）
        if (a.size != b.size) return 0f
        // 初始化点积为 0
        var dotProduct = 0f
        // 遍历向量的每个维度累加点积
        for (i in a.indices) {
            // 累加对应维度的乘积
            dotProduct += a[i] * b[i]
        } // 结束遍历
        // 两侧已归一化，余弦相似度 = 点积；钳位到 [-1, 1] 防止浮点误差
        return dotProduct.coerceIn(-1f, 1f)
    } // 结束 cosineSimilarity 方法

    /**
     * 在图片上绘制检测结果
     */
    // 公开方法：在原始图像上绘制检测框、关键点和标签
    fun drawDetections(bitmap: Bitmap, detections: List<FaceDetection>, names: List<String>? = null): Bitmap {
        // 确保输入 Bitmap 为软件渲染格式
        val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            // 将 HARDWARE 格式复制为 ARGB_8888 格式
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: throw Exception("无法转换 HARDWARE Bitmap")
        // 如果已经是软件格式
        } else {
            // 直接使用原 Bitmap
            bitmap
        } // 结束格式检查
        // 创建可编辑的结果 Bitmap 副本
        val resultBitmap = safeBitmap.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw Exception("无法复制 Bitmap (${safeBitmap.config}, ${safeBitmap.width}x${safeBitmap.height})")
        // 如果安全 Bitmap 不是原始 Bitmap，释放中间 Bitmap
        if (safeBitmap !== bitmap) safeBitmap.recycle()
        // 创建画布绑定到结果 Bitmap
        val canvas = Canvas(resultBitmap)

        // 创建绘制检测框的画笔
        val boxPaint = Paint().apply {
            // 设置颜色为绿色
            color = Color.GREEN
            // 设置绘制样式为描边（空心矩形）
            style = Paint.Style.STROKE
            // 设置线宽，根据图像大小自适应
            strokeWidth = max(3f, bitmap.width / 300f)
            // 启用抗锯齿
            isAntiAlias = true
        } // 结束检测框画笔设置

        // 创建绘制文字标签的画笔
        val textPaint = Paint().apply {
            // 设置颜色为绿色
            color = Color.GREEN
            // 设置文字大小，根据图像大小自适应
            textSize = max(24f, bitmap.width / 50f)
            // 启用抗锯齿
            isAntiAlias = true
            // 启用粗体文字
            isFakeBoldText = true
        } // 结束文字画笔设置

        // 创建绘制标签背景的画笔
        val bgPaint = Paint().apply {
            // 设置半透明黑色背景（alpha=160）
            color = Color.argb(160, 0, 0, 0)
            // 设置绘制样式为填充（实心矩形）
            style = Paint.Style.FILL
        } // 结束背景画笔设置

        // 创建绘制关键点的画笔
        val landmarkPaint = Paint().apply {
            // 设置颜色为黄色
            color = Color.YELLOW
            // 设置绘制样式为填充（实心圆点）
            style = Paint.Style.FILL
            // 设置线宽
            strokeWidth = 3f
        } // 结束关键点画笔设置

        // 遍历所有检测结果并绘制
        detections.forEachIndexed { index, detection ->
            // 绘制检测框
            // 使用绿色画笔绘制人脸边界框
            canvas.drawRect(detection.rect, boxPaint)

            // 绘制关键点
            // 遍历检测结果中的所有关键点
            detection.landmarks.forEach { (lx, ly) ->
                // 在关键点位置绘制黄色实心圆点
                canvas.drawCircle(lx, ly, 4f, landmarkPaint)
            } // 结束关键点绘制

            // 绘制标签背景
            // 获取标签名称（如果提供了名称列表则使用，否则使用默认格式）
            val label = names?.getOrNull(index) ?: "Face ${index + 1}"
            // 将置信度格式化为两位小数字符串
            val scoreText = String.format("%.2f", detection.confidence)
            // 组合标签和置信度为完整文本
            val fullText = "$label ($scoreText)"

            // 测量文本的宽度
            val textWidth = textPaint.measureText(fullText)
            // 计算文本的高度（基线到顶部的距离）
            val textHeight = textPaint.fontMetrics.run { descent - ascent }
            // 定义文字与背景边框的内边距
            val padding = 6f

            // 计算标签背景的左边界
            val bgLeft = detection.rect.left.toFloat()
            // 计算标签背景的上边界（位于检测框上方）
            val bgTop = detection.rect.top.toFloat() - textHeight - padding * 2
            // 计算标签背景的右边界（根据文本宽度自适应）
            val bgRight = bgLeft + textWidth + padding * 2
            // 计算标签背景的下边界（与检测框上边缘对齐）
            val bgBottom = detection.rect.top.toFloat()

            // 绘制半透明黑色背景矩形
            canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, bgPaint)
            // 在背景上绘制绿色文字标签
            canvas.drawText(fullText, bgLeft + padding, bgBottom - padding, textPaint)
        } // 结束检测结果遍历

        // 返回绘制了检测结果的 Bitmap
        return resultBitmap
    } // 结束 drawDetections 方法

    /**
     * 释放资源
     * @Synchronized 防止 close() 与 detectFaces/extractEmbedding 并发执行导致 NPE
     * （如 ViewModel.onCleared 与正在进行的识别并发）。
     */
    // 公开方法：释放所有 ONNX Runtime 资源（会话和环境），加同步保护
    @Synchronized
    fun close() {
        // 打印开始释放资源的日志
        Log.i(TAG, "释放 ONNX Runtime 资源...")
        // 将加载状态设为 false
        isLoaded = false
        // 安全关闭检测模型会话
        try { detSession?.close() } catch (e: Exception) { Log.w(TAG, "关闭检测会话失败: ${e.message}") }
        // 安全关闭识别模型会话
        try { recSession?.close() } catch (e: Exception) { Log.w(TAG, "关闭识别会话失败: ${e.message}") }
        // 将检测会话引用设为 null
        detSession = null
        // 将识别会话引用设为 null
        recSession = null
        // 安全关闭 ONNX Runtime 环境
        try { env?.close() } catch (e: Exception) { Log.w(TAG, "关闭环境失败: ${e.message}") }
        // 将环境引用设为 null
        env = null
        // 打印资源释放完成的日志
        Log.i(TAG, "资源已释放")
    } // 结束 close 方法

    // 定义人脸检测结果的数据类
    data class FaceDetection(
        val rect: Rect, // 人脸边界框矩形
        val confidence: Float, // 检测置信度分数
        val landmarks: List<Pair<Float, Float>> = emptyList() // 5 个关键点坐标列表（左眼、右眼、鼻尖、左嘴角、右嘴角）
    ) // 结束 FaceDetection 数据类

    // 定义人脸识别结果的数据类
    data class RecognitionResult(
        val name: String, // 识别出的人脸名称（未知时为 "UNKNOWN"）
        val confidence: Float // 识别置信度（余弦相似度分数）
    ) // 结束 RecognitionResult 数据类
} // 结束 OnnxFaceRecognition 类
