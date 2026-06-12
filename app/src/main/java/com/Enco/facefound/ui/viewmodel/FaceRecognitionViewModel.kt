package com.Enco.facefound.ui.viewmodel // 声明当前文件所属的包路径，用于组织和管理ViewModel相关的类

import android.app.Application // 导入Android应用类，用于获取应用级别的上下文
import android.content.Context // 导入上下文类，用于访问系统服务和资源
import android.content.SharedPreferences // 导入SharedPreferences类，用于持久化存储用户设置
import android.graphics.Bitmap // 导入位图类，用于处理图片数据
import android.graphics.BitmapFactory // 导入位图工厂类，用于从流中解码生成位图
import android.net.Uri // 导入统一资源标识符类，用于标识图片或视频等资源的路径
import android.os.Build // 导入构建信息类，用于获取Android系统版本号
import android.util.Log // 导入日志工具类，用于输出调试和错误日志
import org.json.JSONArray // 导入JSON数组类，用于序列化和反序列化历史记录列表
import org.json.JSONObject // 导入JSON对象类，用于序列化和反序列化单条历史记录
import androidx.lifecycle.AndroidViewModel // 导入AndroidViewModel基类，它持有Application引用，生命周期感知
import androidx.lifecycle.viewModelScope // 导入ViewModel的作用域，用于管理协程的生命周期
import kotlinx.coroutines.Dispatchers // 导入协程调度器，用于指定协程运行的线程（IO、Main等）
import kotlinx.coroutines.Job // 导入协程任务类，用于控制协程的取消和状态管理
import kotlinx.coroutines.flow.MutableStateFlow // 导入可变状态流，用于持有和更新可观察的状态数据
import kotlinx.coroutines.flow.StateFlow // 导入只读状态流，对外暴露不可变的状态观察接口
import kotlinx.coroutines.flow.update // 导入状态流的update扩展函数，用于原子性地更新状态值
import kotlinx.coroutines.isActive // 导入协程活跃状态检查属性，用于判断协程是否仍在运行
import kotlinx.coroutines.launch // 导入协程启动函数，用于在作用域内启动新的协程
import kotlinx.coroutines.withContext // 导入协程上下文切换函数，用于在不同调度器之间切换执行线程
import com.Enco.facefound.ml.OnnxFaceRecognition // 导入ONNX人脸识别核心类，提供人脸检测和识别功能
import com.Enco.facefound.util.NpzParser // 导入NPZ文件解析器，用于解析NPZ格式的模板文件
import com.Enco.facefound.util.TemplateRepository // 导入模板仓库类，用于持久化存储和加载人脸模板数据
import com.Enco.facefound.video.VideoProcessor // 导入视频处理器类，用于逐帧处理视频中的人脸识别
import java.io.File // 导入文件类，用于操作本地文件系统中的文件
import java.text.SimpleDateFormat // 导入日期格式化类，用于将日期格式化为指定的字符串模式
import java.util.Date // 导入日期类，用于获取当前时间戳
import java.util.Locale // 导入区域设置类，用于指定日期格式化的语言和区域规则

/**
 * 人脸识别 ViewModel
 * 稳定性优先，带完整异常处理和性能监控
 */ // 类的文档注释，说明此ViewModel负责人脸识别的UI状态管理，注重稳定性和异常处理
class FaceRecognitionViewModel(application: Application) : AndroidViewModel(application) { // 定义人脸识别ViewModel类，继承AndroidViewModel以持有Application上下文

    // --- 状态数据类 --- // 分隔注释，标记下方为UI状态相关的数据类定义

    data class UiState( // 定义UI状态数据类，封装所有界面需要展示的状态信息
        val statusMessage: String = "准备就绪", // 状态提示消息，默认显示"准备就绪"
        val isReady: Boolean = false, // 标记模型和环境是否已准备就绪，默认未就绪
        val isModelLoaded: Boolean = false, // 标记ONNX模型是否已成功加载，默认未加载
        val inputImageUri: Uri? = null, // 用户选择的输入图片的URI地址，默认为空
        val resultBitmap: Bitmap? = null, // 识别结果绘制后的位图，包含检测框和名称标注
        val templateName: String? = null, // 当前加载的模板文件名称
        val templateUri: Uri? = null, // 当前加载的模板文件URI地址
        val threshold: Float = 0.3f, // 人脸识别的相似度阈值，高于此值才判定为匹配
        val detectionThreshold: Float = 0.5f, // 人脸检测的置信度阈值，低于此值的检测框被过滤
        val isProcessing: Boolean = false, // 标记是否正在处理识别任务中
        val logs: List<String> = emptyList(), // 运行日志列表，记录操作过程中的所有事件
        val detectedFaces: List<OnnxFaceRecognition.FaceDetection> = emptyList(), // 检测到的人脸列表，包含位置和关键点信息
        val recognitionResults: List<OnnxFaceRecognition.RecognitionResult> = emptyList(), // 识别结果列表，包含匹配的人名和置信度
        val processingTimeMs: Long = 0, // 处理耗时，单位为毫秒
        val isDarkTheme: Boolean = false, // 标记是否使用深色主题，默认为浅色主题
        val currentScreen: Screen = Screen.Main, // 当前显示的页面，默认为主页面
        val templateList: List<TemplateItem> = emptyList(), // 模板列表，展示所有已加载的人脸模板
        val recognitionHistory: List<RecognitionHistoryItem> = emptyList(), // 识别历史记录列表
        val imageDownsample: Boolean = true, // 标记是否对大图片进行降采样处理以节省内存
        val videoUri: Uri? = null, // 用户选择的视频文件URI地址
        val videoInfo: VideoProcessor.VideoInfo? = null, // 视频文件的元信息（宽高、时长等）
        val videoThreshold: Float = 0.35f, // 视频人脸识别的相似度阈值
        val videoDetectionThreshold: Float = 0.5f, // 视频人脸检测的置信度阈值
        val videoSampleRate: Int = 1, // 视频抽帧频率，每N帧处理一帧
        val videoProcessingState: VideoProcessingState = VideoProcessingState.Idle, // 视频处理的当前状态，默认空闲
        val videoProgress: Float = 0f, // 视频处理的进度百分比，范围0到1
        val videoProcessedCount: Int = 0, // 视频已处理的帧数
        val videoProcessedFrames: List<VideoFrameResult> = emptyList(), // 视频已处理帧的结果列表
        val outputVideoUri: Uri? = null // 输出视频文件的URI地址
    ) // 结束UiState数据类的属性定义

    enum class Screen { // 定义页面枚举类，列出应用中所有可导航的页面
        Main, Video, Templates, History, Settings, About // 主页、视频页、模板页、历史页、设置页、关于页
    } // 结束Screen枚举类

    data class TemplateItem( // 定义模板条目数据类，用于在UI中展示单个模板信息
        val name: String, // 模板的名称标识，通常是人名
        val embedding: FloatArray // 人脸特征向量，是一个浮点数数组，用于人脸比对
    ) { // 开始TemplateItem类体
        override fun equals(other: Any?): Boolean { // 重写equals方法，用于比较两个TemplateItem是否相等
            if (this === other) return true // 如果是同一个对象引用，直接返回true
            if (javaClass != other?.javaClass) return false // 如果类型不同，返回false
            other as TemplateItem // 将other强制转换为TemplateItem类型
            if (name != other.name) return false // 如果名称不同，返回false
            if (!embedding.contentEquals(other.embedding)) return false // 如果特征向量内容不同，返回false
            return true // 所有字段都相等，返回true
        } // 结束equals方法

        override fun hashCode(): Int { // 重写hashCode方法，配合equals方法保证哈希集合的正确性
            var result = name.hashCode() // 以名称的哈希值作为初始结果
            result = 31 * result + embedding.contentHashCode() // 将特征向量的哈希值混入结果中
            return result // 返回最终计算的哈希值
        } // 结束hashCode方法
    } // 结束TemplateItem数据类

    data class RecognitionHistoryItem( // 定义识别历史条目数据类，记录每次识别操作的信息
        val id: Long = System.currentTimeMillis(), // 唯一标识符，使用当前时间戳生成
        val timestamp: Date = Date(), // 识别操作发生的时间，默认为当前时间
        val imagePath: String? = null, // 识别图片的本地存储路径，可为空
        val recognizedNames: List<String> = emptyList(), // 本次识别匹配到的所有人名列表
        val processingTimeMs: Long = 0 // 本次识别的处理耗时，单位为毫秒
    ) // 结束RecognitionHistoryItem数据类

    sealed class VideoProcessingState { // 定义视频处理状态密封类，用于表示视频处理的所有可能状态
        data object Idle : VideoProcessingState() // 空闲状态，表示尚未开始处理
        data object Processing : VideoProcessingState() // 处理中状态，表示正在逐帧识别人脸
        data class Encoding(val message: String? = null) : VideoProcessingState() // 编码中状态，可附带进度消息
        data object Completed : VideoProcessingState() // 完成状态，表示视频处理已全部结束
        data class Error(val message: String) : VideoProcessingState() // 错误状态，附带错误描述信息
    } // 结束VideoProcessingState密封类

    data class VideoFrameResult( // 定义视频帧结果数据类，记录单帧的识别结果
        val frameIndex: Int, // 帧的索引编号
        val detections: List<OnnxFaceRecognition.FaceDetection> = emptyList(), // 该帧中检测到的人脸列表
        val names: List<String> = emptyList() // 该帧中识别匹配到的人名列表
    ) // 结束VideoFrameResult数据类

    // --- 状态管理 --- // 分隔注释，标记下方为状态管理相关的属性声明

    private val _uiState = MutableStateFlow(UiState()) // 创建私有的可变状态流，初始值为默认的UiState实例
    val uiState: StateFlow<UiState> = _uiState // 对外暴露只读的状态流，供UI层观察和订阅

    private var faceRecognizer: OnnxFaceRecognition? = null // 人脸识别器实例，初始为空，初始化时创建
    private var templates: Map<String, FloatArray> = emptyMap() // 模板映射表，键为人名，值为对应的特征向量
    private val templateRepo by lazy { TemplateRepository(application) } // 模板仓库实例，使用懒加载方式延迟初始化
    private var videoProcessor: VideoProcessor? = null // 视频处理器实例，处理视频时按需创建
    private var videoJob: Job? = null // 视频处理的协程任务引用，用于取消正在执行的视频处理

    companion object { // 伴生对象，存放类级别的静态成员
        private const val TAG = "FaceRecognitionVM" // 日志标签常量，用于Logcat中过滤本ViewModel的日志输出
        private const val PREFS_NAME = "facefound_settings" // SharedPreferences文件名
        private const val KEY_THRESHOLD = "threshold" // 识别阈值的存储键
        private const val KEY_DETECTION_THRESHOLD = "detection_threshold" // 检测阈值的存储键
        private const val KEY_DARK_THEME = "dark_theme" // 深色主题的存储键
        private const val KEY_IMAGE_DOWNSAMPLE = "image_downsample" // 图片降采样的存储键
        private const val KEY_VIDEO_THRESHOLD = "video_threshold" // 视频识别阈值的存储键
        private const val KEY_VIDEO_DETECTION_THRESHOLD = "video_detection_threshold" // 视频检测阈值的存储键
        private const val KEY_VIDEO_SAMPLE_RATE = "video_sample_rate" // 视频抽帧频率的存储键
        private const val KEY_HISTORY = "recognition_history" // 识别历史记录的存储键
    } // 结束companion object

    private val prefs: SharedPreferences by lazy { // 懒加载SharedPreferences实例，用于持久化存储用户设置
        getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) // 获取或创建名为facefound_settings的SharedPreferences文件
    } // 结束prefs属性

    // --- 初始化 --- // 分隔注释，标记下方为初始化相关的函数

    fun initialize() { // 初始化函数，负责加载ONNX模型和已有模板
        if (faceRecognizer?.isLoaded == true) return // 如果人脸识别器已加载，直接返回避免重复初始化
        loadSettings() // 从SharedPreferences加载上次保存的用户设置
        loadHistory() // 从SharedPreferences加载上次保存的识别历史记录
        val context = getApplication<Application>() // 获取应用上下文，用于模型文件的读取
        viewModelScope.launch { // 在ViewModel作用域内启动协程，生命周期与ViewModel绑定
            _uiState.update { it.copy(statusMessage = "正在加载模型...") } // 更新状态消息为"正在加载模型..."

            val result = runCatching { // 使用runCatching捕获可能的异常，返回Result包装
                withContext(Dispatchers.IO) { // 切换到IO线程执行模型加载，避免阻塞主线程
                    OnnxFaceRecognition(context) // 在IO线程中创建ONNX人脸识别实例
                } // 结束withContext IO线程切换
            } // 结束runCatching异常捕获

            val newRecognizer = result.getOrNull() // 从Result中获取成功的结果，失败则返回null
            if (newRecognizer != null) { // 如果模型加载成功
                faceRecognizer?.close() // 关闭旧的人脸识别器实例，释放资源
                faceRecognizer = newRecognizer // 将新加载的识别器赋值给成员变量
            } // 结束模型实例替换
            val isLoaded = faceRecognizer?.isLoaded ?: false // 检查人脸识别器是否成功加载，未初始化则为false

            _uiState.update { // 更新UI状态
                it.copy( // 复制当前状态并修改以下字段
                    isModelLoaded = isLoaded, // 设置模型加载状态
                    isReady = isLoaded, // 设置就绪状态与模型加载状态一致
                    statusMessage = if (isLoaded) "模型已加载" else "模型加载失败" // 根据加载结果设置状态消息
                ) // 结束copy
            } // 结束_uiState.update

            if (isLoaded) { // 如果模型加载成功
                addLog("✅ 模型加载成功") // 添加成功日志

                val savedTemplates = templateRepo.loadAll() // 从模板仓库加载所有已保存的模板数据
                val validTemplates = savedTemplates.filter { it.value.size == OnnxFaceRecognition.EMBEDDING_DIM } // 过滤出维度正确的有效模板
                if (validTemplates.size < savedTemplates.size) { // 如果存在无效模板（维度不匹配）
                    val invalidCount = savedTemplates.size - validTemplates.size // 计算无效模板的数量
                    addLog("⚠️ 过滤掉 $invalidCount 个无效模板（维度不匹配，应为${OnnxFaceRecognition.EMBEDDING_DIM}维）") // 记录过滤无效模板的日志
                    templateRepo.save(validTemplates) // 将过滤后的有效模板重新保存，清除无效数据
                } // 结束无效模板处理
                if (validTemplates.isNotEmpty()) { // 如果存在有效的模板
                    templates = validTemplates // 将有效模板赋值给成员变量
                    val templateList = validTemplates.map { (name, emb) -> TemplateItem(name, emb) } // 将模板映射转为UI展示用的列表
                    _uiState.update { it.copy(templateList = templateList) } // 更新UI中的模板列表
                    addLog("📂 已自动加载 ${validTemplates.size} 个模板，无需二次导入") // 记录自动加载模板的日志
                } else { // 如果没有有效模板
                    addLog("💡 请导入 NPZ 模板文件，导入后将自动保存") // 提示用户导入NPZ模板文件
                } // 结束模板加载判断
            } else { // 如果模型加载失败
                val error = result.exceptionOrNull()?.message ?: "未知错误" // 获取异常的错误消息
                addLog("❌ 模型加载失败: $error") // 记录模型加载失败的日志
            } // 结束模型加载结果处理
        } // 结束viewModelScope.launch协程
    } // 结束initialize函数

    // --- 设置持久化 --- // 分隔注释，标记下方为设置持久化相关的函数

    private fun loadSettings() { // 从SharedPreferences加载上次保存的用户设置，恢复到UI状态中
        _uiState.update { // 更新UI状态，将SharedPreferences中读取的值覆盖默认值
            it.copy( // 复制当前状态并修改以下设置字段
                threshold = prefs.getFloat(KEY_THRESHOLD, 0.3f), // 读取识别阈值，默认0.3
                detectionThreshold = prefs.getFloat(KEY_DETECTION_THRESHOLD, 0.5f), // 读取检测阈值，默认0.5
                isDarkTheme = prefs.getBoolean(KEY_DARK_THEME, false), // 读取深色主题设置，默认关闭
                imageDownsample = prefs.getBoolean(KEY_IMAGE_DOWNSAMPLE, true), // 读取图片降采样设置，默认开启
                videoThreshold = prefs.getFloat(KEY_VIDEO_THRESHOLD, 0.35f), // 读取视频识别阈值，默认0.35
                videoDetectionThreshold = prefs.getFloat(KEY_VIDEO_DETECTION_THRESHOLD, 0.5f), // 读取视频检测阈值，默认0.5
                videoSampleRate = prefs.getInt(KEY_VIDEO_SAMPLE_RATE, 1) // 读取视频抽帧频率，默认1
            ) // 结束copy
        } // 结束update
    } // 结束loadSettings函数

    private fun saveSettings() { // 将当前UI状态中的设置项保存到SharedPreferences，实现持久化存储
        prefs.edit() // 获取SharedPreferences编辑器
            .putFloat(KEY_THRESHOLD, _uiState.value.threshold) // 保存识别阈值
            .putFloat(KEY_DETECTION_THRESHOLD, _uiState.value.detectionThreshold) // 保存检测阈值
            .putBoolean(KEY_DARK_THEME, _uiState.value.isDarkTheme) // 保存深色主题设置
            .putBoolean(KEY_IMAGE_DOWNSAMPLE, _uiState.value.imageDownsample) // 保存图片降采样设置
            .putFloat(KEY_VIDEO_THRESHOLD, _uiState.value.videoThreshold) // 保存视频识别阈值
            .putFloat(KEY_VIDEO_DETECTION_THRESHOLD, _uiState.value.videoDetectionThreshold) // 保存视频检测阈值
            .putInt(KEY_VIDEO_SAMPLE_RATE, _uiState.value.videoSampleRate) // 保存视频抽帧频率
            .apply() // 异步写入磁盘，不阻塞主线程
    } // 结束saveSettings函数

    private fun loadHistory() { // 从SharedPreferences加载识别历史记录，恢复到UI状态中
        val jsonStr = prefs.getString(KEY_HISTORY, null) // 读取历史记录的JSON字符串，不存在则返回null
        if (jsonStr.isNullOrEmpty()) return // 如果没有保存的历史记录，直接返回
        try { // 尝试解析JSON
            val jsonArray = JSONArray(jsonStr) // 将JSON字符串解析为JSONArray
            val historyList = mutableListOf<RecognitionHistoryItem>() // 创建可变历史记录列表
            for (i in 0 until jsonArray.length()) { // 遍历JSON数组中的每个元素
                val obj = jsonArray.getJSONObject(i) // 获取第i个JSON对象
                val namesArray = obj.getJSONArray("names") // 获取人名JSON数组
                val names = mutableListOf<String>() // 创建可变人名列表
                for (j in 0 until namesArray.length()) { // 遍历人名数组
                    names.add(namesArray.getString(j)) // 将人名添加到列表中
                } // 结束人名遍历
                historyList.add( // 添加一条历史记录
                    RecognitionHistoryItem( // 创建历史记录条目
                        id = obj.getLong("id"), // 读取唯一标识符
                        timestamp = Date(obj.getLong("timestamp")), // 读取时间戳并转为Date对象
                        recognizedNames = names, // 设置人名列表
                        processingTimeMs = obj.getLong("processingTimeMs") // 读取处理耗时
                    ) // 结束RecognitionHistoryItem创建
                ) // 结束add
            } // 结束遍历
            _uiState.update { it.copy(recognitionHistory = historyList) } // 将加载的历史记录更新到UI状态
        } catch (e: Exception) { // 捕获JSON解析异常
            Log.e(TAG, "加载历史记录失败: ${e.message}") // 记录错误日志
        } // 结束异常捕获
    } // 结束loadHistory函数

    private fun saveHistory() { // 将当前识别历史记录保存到SharedPreferences，实现持久化存储
        try { // 尝试序列化历史记录
            val historyList = _uiState.value.recognitionHistory // 获取当前历史记录列表
            val jsonArray = JSONArray() // 创建JSON数组用于存储序列化后的历史记录
            for (item in historyList) { // 遍历每条历史记录
                val obj = JSONObject() // 创建JSON对象存储单条记录
                obj.put("id", item.id) // 保存唯一标识符
                obj.put("timestamp", item.timestamp.time) // 将Date转为时间戳保存
                obj.put("names", JSONArray(item.recognizedNames)) // 将人名列表转为JSON数组保存
                obj.put("processingTimeMs", item.processingTimeMs) // 保存处理耗时
                jsonArray.put(obj) // 将JSON对象添加到JSON数组中
            } // 结束遍历
            prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply() // 将JSON数组序列化为字符串并保存
        } catch (e: Exception) { // 捕获序列化异常
            Log.e(TAG, "保存历史记录失败: ${e.message}") // 记录错误日志
        } // 结束异常捕获
    } // 结束saveHistory函数

    // --- 屏幕导航 --- // 分隔注释，标记下方为页面导航相关函数

    fun navigateTo(screen: Screen) { // 页面导航函数，切换当前显示的页面
        _uiState.update { it.copy(currentScreen = screen) } // 更新状态中的当前页面为目标页面
    } // 结束navigateTo函数

    // --- 主题切换 --- // 分隔注释，标记下方为主题切换相关函数

    fun toggleTheme() { // 主题切换函数，在深色和浅色主题之间切换
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) } // 将深色主题标志取反，实现切换
        saveSettings() // 将主题设置保存到SharedPreferences
    } // 结束toggleTheme函数

    // --- 设置变更 --- // 分隔注释，标记下方为设置变更相关函数

    fun updateThreshold(value: Float) { // 更新识别阈值函数
        _uiState.update { it.copy(threshold = value) } // 将识别阈值更新为传入的新值
        saveSettings() // 将识别阈值保存到SharedPreferences
    } // 结束updateThreshold函数

    fun updateDetectionThreshold(value: Float) { // 更新检测阈值函数
        _uiState.update { it.copy(detectionThreshold = value) } // 将检测阈值更新为传入的新值
        saveSettings() // 将检测阈值保存到SharedPreferences
    } // 结束updateDetectionThreshold函数

    fun setImageDownsample(enabled: Boolean) { // 设置图片降采样开关函数
        _uiState.update { it.copy(imageDownsample = enabled) } // 更新图片降采样开关的状态
        saveSettings() // 将图片降采样设置保存到SharedPreferences
    } // 结束setImageDownsample函数

    // --- 图片操作 --- // 分隔注释，标记下方为图片操作相关函数

    fun setInputImage(uri: Uri) { // 设置输入图片函数，当用户选择新图片时调用
        val oldBitmap = _uiState.value.resultBitmap // 保存旧的结果位图引用，以便后续回收
        _uiState.update { // 更新UI状态
            it.copy( // 复制当前状态并修改以下字段
                inputImageUri = uri, // 设置新的输入图片URI
                resultBitmap = null, // 清空旧的结果位图
                statusMessage = "图片已加载" // 更新状态消息为"图片已加载"
            ) // 结束copy
        } // 结束_uiState.update
        oldBitmap?.recycle() // 回收旧的结果位图，释放内存
        addLog("📷 加载图片: ${uri.lastPathSegment}") // 记录加载图片的日志，显示文件名
    } // 结束setInputImage函数

    fun startBatchRecognition(uris: List<Uri>) { // 批量识别函数，处理多张图片
        val recognizer = faceRecognizer ?: return // 获取人脸识别器实例，为空则直接返回
        val currentState = _uiState.value // 获取当前UI状态快照
        if (!currentState.isModelLoaded) return // 如果模型未加载，直接返回
        if (uris.isEmpty()) return // 如果图片列表为空，直接返回

        val appContext = getApplication<Application>() // 获取应用上下文
        viewModelScope.launch { // 在ViewModel作用域内启动协程
            _uiState.update { it.copy(isProcessing = true, statusMessage = "批量识别中... 0/${uris.size}") } // 更新状态为处理中
            addLog("📷 开始批量识别: ${uris.size} 张图片") // 记录批量识别开始的日志

            val allHistoryItems = mutableListOf<RecognitionHistoryItem>() // 创建批量识别的历史记录列表
            var processedCount = 0 // 已处理图片计数
            var successCount = 0 // 成功识别的图片计数

            for (uri in uris) { // 遍历每张图片
                try { // 尝试处理当前图片
                    val sourceBitmap = withContext(Dispatchers.IO) { // 在IO线程中加载图片
                        loadBitmapFromUri(appContext, uri) // 从URI加载位图
                    } // 结束IO线程
                    if (sourceBitmap == null) { // 如果图片加载失败
                        addLog("⚠️ 跳过: ${uri.lastPathSegment} (加载失败)") // 记录跳过日志
                        processedCount++ // 递增已处理计数
                        continue // 跳过当前图片，继续下一张
                    } // 结束加载失败检查

                    val maxDimension = 2048 // 定义图片最大尺寸限制
                    val workingBitmap = if (sourceBitmap.width > maxDimension || sourceBitmap.height > maxDimension) { // 如果图片超过最大尺寸
                        val scale = maxDimension.toFloat() / maxOf(sourceBitmap.width, sourceBitmap.height) // 计算缩放比例
                        val newWidth = (sourceBitmap.width * scale).toInt() // 计算缩放后宽度
                        val newHeight = (sourceBitmap.height * scale).toInt() // 计算缩放后高度
                        val scaled = Bitmap.createScaledBitmap(sourceBitmap, newWidth, newHeight, true) // 创建缩放后的位图
                        sourceBitmap.recycle() // 回收原始位图
                        scaled // 使用缩放后的位图
                    } else { // 如果图片尺寸在限制范围内
                        sourceBitmap // 直接使用原图
                    } // 结束图片尺寸处理

                    val detections = recognizer.detectFaces(workingBitmap, currentState.detectionThreshold) // 执行人脸检测
                    val names = if (detections.isNotEmpty()) { // 如果检测到人脸
                        val results = recognizer.recognizeFacesParallel(workingBitmap, detections, templates, currentState.threshold) // 执行人脸识别
                        results.map { it.name } // 提取识别结果中的人名列表
                    } else { // 如果未检测到人脸
                        emptyList() // 返回空人名列表
                    } // 结束人脸检测与识别

                    workingBitmap.recycle() // 释放工作位图内存

                    val historyItem = RecognitionHistoryItem( // 创建当前图片的历史记录条目
                        timestamp = Date(), // 记录当前时间
                        recognizedNames = names, // 记录识别到的人名列表
                        processingTimeMs = 0 // 批量模式不单独记录耗时
                    ) // 结束RecognitionHistoryItem创建
                    allHistoryItems.add(historyItem) // 将当前图片的历史记录添加到列表中

                    processedCount++ // 递增已处理计数
                    if (names.isNotEmpty()) successCount++ // 如果识别到人脸，递增成功计数
                    _uiState.update { it.copy(statusMessage = "批量识别中... $processedCount/${uris.size}") } // 更新处理进度
                    addLog("✅ ${uri.lastPathSegment}: ${names.joinToString(", ").ifEmpty { "未检测到人脸" }}") // 记录当前图片的识别结果
                } catch (e: OutOfMemoryError) { // 捕获内存不足异常
                    addLog("❌ ${uri.lastPathSegment}: 内存不足，跳过") // 记录内存不足日志
                    processedCount++ // 递增已处理计数
                    System.gc() // 建议垃圾回收器回收内存
                } catch (e: Exception) { // 捕获其他异常
                    addLog("❌ ${uri.lastPathSegment}: ${e.message}") // 记录错误日志
                    processedCount++ // 递增已处理计数
                } // 结束异常捕获
            } // 结束图片遍历

            _uiState.update { state -> // 更新UI状态，将批量识别的历史记录合并到现有历史中
                state.copy( // 复制当前状态
                    isProcessing = false, // 标记处理结束
                    statusMessage = "批量识别完成: $successCount/${uris.size} 张识别到人脸", // 更新状态消息显示批量识别结果
                    recognitionHistory = allHistoryItems + state.recognitionHistory.take(50 - allHistoryItems.size) // 将批量历史记录添加到历史头部，总条数限制50条
                ) // 结束copy
            } // 结束_uiState.update
            saveHistory() // 将更新后的历史记录保存到SharedPreferences
            addLog("✅ 批量识别完成: 共${uris.size}张，$successCount张识别到人脸") // 记录批量识别完成的日志
        } // 结束viewModelScope.launch协程
    } // 结束startBatchRecognition函数

    // --- 模板管理 --- // 分隔注释，标记下方为模板管理相关函数

    fun setTemplate(uri: Uri) { // 设置并解析模板文件函数
        val appContext = getApplication<Application>() // 获取应用上下文
        _uiState.update { // 更新UI状态
            it.copy( // 复制当前状态并修改以下字段
                templateUri = uri, // 设置模板文件的URI
                templateName = uri.lastPathSegment // 设置模板文件名作为显示名称
            ) // 结束copy
        } // 结束_uiState.update
        addLog("📂 正在解析模板: ${uri.lastPathSegment}") // 记录正在解析模板的日志

        viewModelScope.launch { // 在ViewModel作用域内启动协程
            val parsedTemplates = NpzParser.parseFromUri(appContext, uri) // 使用NPZ解析器从URI解析模板数据

            if (parsedTemplates.isNotEmpty()) { // 如果成功解析出模板
                val mergedTemplates = templates.toMutableMap().apply { // 创建当前模板的可变副本
                    putAll(parsedTemplates) // 将新解析的模板合并到现有模板中
                } // 结束合并操作
                templates = mergedTemplates // 将合并后的模板赋值给成员变量
                _uiState.update { state -> // 更新UI状态
                    state.copy( // 复制当前状态并修改以下字段
                        templateName = "${uri.lastPathSegment} (${parsedTemplates.size}人)", // 更新模板名称，附带人数信息
                        templateList = templates.map { (name, emb) -> TemplateItem(name, emb) } // 更新模板列表
                    ) // 结束copy
                } // 结束_uiState.update
                viewModelScope.launch(Dispatchers.IO) { templateRepo.save(templates) } // 在IO线程中将模板持久化保存
                addLog("✅ 模板加载并保存成功: ${parsedTemplates.size} 个新模板，当前共 ${templates.size} 个模板") // 记录模板加载成功的日志
            } else { // 如果模板解析失败
                _uiState.update { state -> // 更新UI状态
                    state.copy( // 复制当前状态
                        templateName = "${uri.lastPathSegment} (解析失败)" // 更新模板名称显示解析失败
                    ) // 结束copy
                } // 结束_uiState.update
                addLog("❌ 模板解析失败，请检查文件格式") // 记录模板解析失败的日志
            } // 结束模板解析结果判断
        } // 结束viewModelScope.launch协程
    } // 结束setTemplate函数

    fun removeTemplate(name: String) { // 删除指定名称的模板函数
        templates = templates.filter { it.key != name } // 从模板映射中过滤掉指定名称的模板
        _uiState.update { state -> // 更新UI状态
            state.copy( // 复制当前状态
                templateList = state.templateList.filter { it.name != name } // 从模板列表中过滤掉指定名称的模板
            ) // 结束copy
        } // 结束_uiState.update
        viewModelScope.launch(Dispatchers.IO) { templateRepo.save(templates) } // 在IO线程中保存更新后的模板数据
        addLog("❌ 已删除模板: $name") // 记录删除模板的日志
    } // 结束removeTemplate函数

    fun renameTemplate(oldName: String, newName: String) { // 重命名模板函数
        val embedding = templates[oldName] ?: return // 获取旧名称对应的特征向量，不存在则直接返回
        val newTemplates = templates.toMutableMap().apply { // 创建当前模板的可变副本
            remove(oldName) // 移除旧名称的条目
            put(newName, embedding) // 以新名称重新插入相同的特征向量
        } // 结束重命名操作
        templates = newTemplates // 将重命名后的模板赋值给成员变量
        _uiState.update { state -> // 更新UI状态
            state.copy( // 复制当前状态
                templateList = state.templateList.map { // 遍历模板列表
                    if (it.name == oldName) it.copy(name = newName) else it // 找到旧名称的条目则替换为新名称，否则保持不变
                } // 结束map遍历
            ) // 结束copy
        } // 结束_uiState.update
        viewModelScope.launch(Dispatchers.IO) { templateRepo.save(templates) } // 在IO线程中保存更新后的模板数据
        addLog("✏️ 模板已重命名: $oldName → $newName") // 记录模板重命名的日志
    } // 结束renameTemplate函数

    fun addTemplateFromFace( // 从人脸位图直接添加模板的函数
        name: String, // 模板的名称标识
        faceBitmap: Bitmap // 人脸区域的位图数据
    ) { // 开始函数体
        viewModelScope.launch { // 在ViewModel作用域内启动协程
            try { // 尝试执行以下操作
                val embedding = faceRecognizer?.extractEmbedding(faceBitmap) ?: return@launch // 从人脸位图中提取特征向量，失败则退出协程
                templates = templates.toMutableMap().apply { put(name, embedding) } // 将新的名称和特征向量添加到模板映射中
                _uiState.update { state -> // 更新UI状态
                    state.copy( // 复制当前状态
                        templateList = state.templateList + TemplateItem(name, embedding) // 在模板列表末尾追加新的模板条目
                    ) // 结束copy
                } // 结束_uiState.update
                withContext(Dispatchers.IO) { templateRepo.save(templates) } // 在IO线程中将模板持久化保存
                addLog("✅ 已添加新模板: $name") // 记录添加模板成功的日志
            } catch (e: Exception) { // 捕获异常
                addLog("❌ 添加模板失败: ${e.message}") // 记录添加模板失败的日志
            } // 结束try-catch
        } // 结束viewModelScope.launch协程
    } // 结束addTemplateFromFace函数

    // --- 识别功能 --- // 分隔注释，标记下方为识别功能相关函数

    fun startRecognition() { // 开始人脸识别函数
        val appContext = getApplication<Application>() // 获取应用上下文
        val currentState = _uiState.value // 获取当前的UI状态快照

        if (!currentState.isModelLoaded) { // 如果模型尚未加载
            _uiState.update { it.copy(statusMessage = "模型未加载") } // 更新状态消息提示模型未加载
            addLog("⚠️ 请先加载模型") // 记录需要先加载模型的日志
            return // 提前返回，不执行后续识别逻辑
        } // 结束模型加载检查

        if (currentState.inputImageUri == null) { // 如果尚未选择输入图片
            _uiState.update { it.copy(statusMessage = "请先选择图片") } // 更新状态消息提示先选择图片
            addLog("⚠️ 请先选择图片") // 记录需要先选择图片的日志
            return // 提前返回，不执行后续识别逻辑
        } // 结束图片选择检查

        viewModelScope.launch { // 在ViewModel作用域内启动协程
            val totalStartTime = System.currentTimeMillis() // 记录识别流程开始的总时间戳

            // 先回收旧的 bitmap // 注释说明下方代码用于回收旧的结果位图
            val oldBitmap = _uiState.value.resultBitmap // 获取旧的结果位图引用
            _uiState.update { // 更新UI状态
                it.copy( // 复制当前状态并修改以下字段
                    isProcessing = true, // 标记正在处理中
                    statusMessage = "正在识别...", // 更新状态消息为正在识别
                    resultBitmap = null // 清空旧的结果位图
                ) // 结束copy
            } // 结束_uiState.update
            oldBitmap?.recycle() // 回收旧的结果位图，释放内存

            var sourceBitmap: Bitmap? = null // 声明源图片位图变量，用于后续可能的资源释放
            var sourceBitmapTransferred = false // 标记源位图是否已转移给UI状态，避免重复回收

            try { // 尝试执行识别流程
                sourceBitmap = loadBitmapFromUri(appContext, currentState.inputImageUri) // 从URI加载源图片位图
                if (sourceBitmap == null) { // 如果图片加载失败
                    throw Exception("无法加载图片，可能格式不支持") // 抛出异常中止流程
                } // 结束图片加载失败检查

                val maxDimension = if (currentState.imageDownsample) 4096 else 8192 // 根据降采样设置确定图片最大尺寸
                if (sourceBitmap.width > maxDimension || sourceBitmap.height > maxDimension) { // 如果图片尺寸超过最大限制
                    val scale = maxDimension.toFloat() / maxOf(sourceBitmap.width, sourceBitmap.height) // 计算缩放比例
                    val newWidth = (sourceBitmap.width * scale).toInt() // 计算缩放后的宽度
                    val newHeight = (sourceBitmap.height * scale).toInt() // 计算缩放后的高度
                    val scaled = Bitmap.createScaledBitmap(sourceBitmap, newWidth, newHeight, true) // 创建缩放后的位图
                    sourceBitmap.recycle() // 回收原始大小的位图
                    sourceBitmap = scaled // 将缩放后的位图赋值给源位图变量
                    addLog("📐 图片已缩放至 ${newWidth}x${newHeight}") // 记录图片缩放的日志
                } // 结结束图片缩放处理

                addLog("🔍 开始人脸检测...") // 记录开始人脸检测的日志

                val detections = faceRecognizer?.detectFaces(sourceBitmap, currentState.detectionThreshold) ?: emptyList() // 对源图片执行人脸检测
                _uiState.update { it.copy(detectedFaces = detections) } // 更新状态中的检测到的人脸列表
                addLog("✅ 检测到 ${detections.size} 张人脸") // 记录检测到的人脸数量

                if (detections.isEmpty()) { // 如果未检测到人脸
                    _uiState.update { state -> // 更新UI状态
                        state.copy( // 复制当前状态
                            isProcessing = false, // 标记处理结束
                            statusMessage = "未检测到人脸", // 更新状态消息为未检测到人脸
                            resultBitmap = sourceBitmap // 将源图片作为结果位图展示
                        ) // 结束copy
                    } // 结束_uiState.update
                    sourceBitmapTransferred = true // 标记源位图已转移给UI状态，避免在finally中被回收
                    addLog("⚠️ 未检测到人脸") // 记录未检测到人脸的日志
                    return@launch // 退出协程
                } // 结束空检测结果处理

                addLog("🔍 并行处理 ${detections.size} 张人脸...") // 记录开始并行处理

                val results = faceRecognizer?.recognizeFacesParallel(
                    sourceBitmap, detections, templates, currentState.threshold
                ) ?: List(detections.size) { OnnxFaceRecognition.RecognitionResult("UNKNOWN", 0f) }

                val recognizedNames = results.map { it.name }.toMutableList()

                results.forEachIndexed { index, result ->
                    addLog("✅ 人脸 ${index + 1}: ${result.name} (${String.format("%.2f", result.confidence)})")
                }

                val resultBitmap = faceRecognizer?.drawDetections(sourceBitmap, detections, recognizedNames) // 在源图片上绘制检测框和识别名称
                val totalTime = System.currentTimeMillis() - totalStartTime // 计算整个识别过程的总耗时

                val historyItem = RecognitionHistoryItem( // 创建识别历史条目
                    timestamp = Date(), // 记录当前时间
                    recognizedNames = recognizedNames, // 记录识别到的人名列表
                    processingTimeMs = totalTime // 记录处理耗时
                ) // 结束RecognitionHistoryItem创建

                _uiState.update { state -> // 更新UI状态
                    state.copy( // 复制当前状态并修改以下字段
                        recognitionResults = results, // 更新识别结果列表
                        resultBitmap = resultBitmap, // 更新结果位图（包含标注）
                        isProcessing = false, // 标记处理结束
                        statusMessage = "识别完成: ${results.size} 张人脸 (${totalTime}ms)", // 更新状态消息显示识别完成
                        processingTimeMs = totalTime, // 更新处理耗时
                        recognitionHistory = listOf(historyItem) + state.recognitionHistory.take(49) // 将新记录添加到历史记录头部，最多保留50条
                    ) // 结束copy
                } // 结束_uiState.update
                saveHistory() // 将更新后的历史记录保存到SharedPreferences

                addLog("✅ 识别完成，总耗时 ${totalTime}ms") // 记录识别完成的日志

            } catch (e: OutOfMemoryError) { // 捕获整体流程的内存不足错误
                Log.e(TAG, "内存不足", e) // 输出OOM错误日志到Logcat
                _uiState.update { state -> // 更新UI状态
                    state.copy( // 复制当前状态
                        isProcessing = false, // 标记处理结束
                        statusMessage = "内存不足，请尝试更小的图片" // 更新状态消息提示内存不足
                    ) // 结束copy
                } // 结束_uiState.update
                addLog("❌ 内存不足: ${e.message}") // 记录内存不足的日志
                System.gc() // 建议垃圾回收器回收内存
            } catch (e: Exception) { // 捕获其他一般异常
                Log.e(TAG, "识别失败", e) // 输出识别失败的错误日志到Logcat
                _uiState.update { state -> // 更新UI状态
                    state.copy( // 复制当前状态
                        isProcessing = false, // 标记处理结束
                        statusMessage = "识别失败: ${e.message}" // 更新状态消息显示失败原因
                    ) // 结束copy
                } // 结束_uiState.update
                addLog("❌ 识别失败: ${e.javaClass.simpleName}: ${e.message}") // 记录识别失败的详细日志
            } catch (e: Throwable) { // 捕获严重错误（如StackOverflow等Error级别异常）
                Log.e(TAG, "识别严重错误", e) // 输出严重错误日志到Logcat
                _uiState.update { state -> // 更新UI状态
                    state.copy( // 复制当前状态
                        isProcessing = false, // 标记处理结束
                        statusMessage = "严重错误: ${e.message}" // 更新状态消息显示严重错误信息
                    ) // 结束copy
                } // 结束_uiState.update
                addLog("❌ 严重错误: ${e.javaClass.simpleName}: ${e.message}") // 记录严重错误的详细日志
            } finally { // 无论是否异常都会执行的清理代码
                if (sourceBitmap != null && !sourceBitmapTransferred) { // 如果源位图不为空且未转移给UI状态
                    sourceBitmap.recycle() // 回收源位图，释放内存
                } // 结束源位图回收检查
            } // 结束try-catch-finally
        } // 结束viewModelScope.launch协程
    } // 结束startRecognition函数

    fun saveResultImage() { // 保存识别结果图片到系统相册的函数
        val appContext = getApplication<Application>() // 获取应用上下文
        val bitmap = _uiState.value.resultBitmap?.copy(Bitmap.Config.ARGB_8888, false) ?: return // 复制结果位图以便安全保存，如果为空则直接返回
        viewModelScope.launch(Dispatchers.IO) { // 在IO线程中启动协程执行保存操作
            try { // 尝试执行保存操作
                val fileName = "FaceFound_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg" // 生成带时间戳的文件名
                val resolver = appContext.contentResolver // 获取内容解析器，用于操作MediaStore
                val contentValues = android.content.ContentValues().apply { // 创建ContentValues用于插入MediaStore记录
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName) // 设置文件显示名称
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg") // 设置MIME类型为JPEG图片
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // 如果Android版本 >= 10（API 29）
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FaceFound") // 设置相对路径为Pictures/FaceFound
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 1) // 标记为待处理状态，防止在写入完成前被其他应用访问
                    } // 结束Android Q以上的额外设置
                } // 结束ContentValues创建

                val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) // 在MediaStore中插入新记录并获取URI
                if (uri != null) { // 如果插入成功获取到了URI
                    resolver.openOutputStream(uri)?.use { out -> // 打开输出流
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) // 将位图以95%质量的JPEG格式压缩写入输出流
                    } // 结束输出流使用
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // 如果Android版本 >= 10
                        contentValues.clear() // 清空之前的ContentValues
                        contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0) // 将待处理标记设为0，表示写入完成
                        resolver.update(uri, contentValues, null, null) // 更新MediaStore记录，解除待处理状态
                    } // 结束Android Q以上的状态更新
                    addLog("💾 图片已保存到相册: $fileName") // 记录保存成功的日志
                    withContext(Dispatchers.Main) { // 切换到主线程更新UI
                        _uiState.update { it.copy(statusMessage = "图片已保存到相册") } // 更新状态消息为保存成功
                    } // 结束主线程UI更新
                } else { // 如果URI插入失败
                    addLog("❌ 无法创建相册条目") // 记录创建相册条目失败的日志
                } // 结束URI有效性检查
            } catch (e: Exception) { // 捕获保存过程中的异常
                addLog("❌ 保存图片失败: ${e.message}") // 记录保存图片失败的日志
            } finally { // 无论是否异常都会执行的清理代码
                bitmap.recycle() // 回收复制的位图，释放内存
            } // 结束try-catch-finally
        } // 结束viewModelScope.launch协程
    } // 结束saveResultImage函数

    fun clearHistory() { // 清空识别历史记录函数
        _uiState.update { // 更新UI状态
            it.copy(recognitionHistory = emptyList()) // 将识别历史记录列表设为空列表
        } // 结束_uiState.update
        saveHistory() // 将清空后的历史记录保存到SharedPreferences
        addLog("🗑️ 识别历史已清空") // 记录历史记录已清空的日志
    } // 结束clearHistory函数

    // --- 视频识别功能 --- // 分隔注释，标记下方为视频识别相关函数

    fun setVideoUri(uri: Uri) { // 设置视频URI函数，当用户选择新视频时调用
        val appContext = getApplication<Application>() // 获取应用上下文
        if (videoProcessor == null) { // 如果视频处理器尚未初始化
            videoProcessor = VideoProcessor(faceRecognizer ?: return) // 使用人脸识别器创建视频处理器，识别器为空则直接返回
        } // 结束视频处理器初始化

        val info = videoProcessor?.getVideoInfo(appContext, uri) // 获取视频文件的元信息
        _uiState.update { // 更新UI状态
            it.copy( // 复制当前状态并修改以下字段
                videoUri = uri, // 设置视频URI
                videoInfo = info, // 设置视频元信息
                videoProcessingState = VideoProcessingState.Idle, // 重置视频处理状态为空闲
                videoProgress = 0f, // 重置进度为0
                videoProcessedCount = 0, // 重置已处理帧数为0
                videoProcessedFrames = emptyList(), // 清空已处理帧结果
                outputVideoUri = null // 清空输出视频URI
            ) // 结束copy
        } // 结束_uiState.update
        addLog("🎬 已加载视频: ${uri.lastPathSegment}") // 记录加载视频的日志
        if (info != null) { // 如果成功获取到视频信息
            addLog("📐 视频信息: ${info.width}x${info.height}, ${info.durationMs}ms") // 记录视频的宽高和时长信息
        } // 结束视频信息日志
    } // 结束setVideoUri函数

    fun updateVideoThreshold(value: Float) { // 更新视频识别阈值函数
        _uiState.update { it.copy(videoThreshold = value) } // 将视频识别阈值更新为传入的新值
        saveSettings() // 将视频识别阈值保存到SharedPreferences
    } // 结束updateVideoThreshold函数

    fun updateVideoDetectionThreshold(value: Float) { // 更新视频检测阈值函数
        _uiState.update { it.copy(videoDetectionThreshold = value) } // 将视频检测阈值更新为传入的新值
        saveSettings() // 将视频检测阈值保存到SharedPreferences
    } // 结束updateVideoDetectionThreshold函数

    fun updateVideoSampleRate(value: Int) { // 更新视频抽帧频率函数
        _uiState.update { it.copy(videoSampleRate = value.coerceIn(1, 5)) } // 将抽帧频率限制在1到5之间并更新
        saveSettings() // 将视频抽帧频率保存到SharedPreferences
    } // 结束updateVideoSampleRate函数

    fun startVideoProcessing() { // 开始视频处理函数
        val appContext = getApplication<Application>() // 获取应用上下文
        val currentState = _uiState.value // 获取当前的UI状态快照

        if (currentState.videoUri == null) { // 如果尚未选择视频文件
            addLog("⚠️ 请先选择视频文件") // 记录提示选择视频文件的日志
            return // 提前返回
        } // 结束视频文件检查

        if (!currentState.isModelLoaded) { // 如果模型尚未加载
            addLog("⚠️ 模型未加载") // 记录模型未加载的日志
            return // 提前返回
        } // 结束模型加载检查

        videoJob?.cancel() // 取消之前正在执行的视频处理任务
        videoJob = viewModelScope.launch { // 在ViewModel作用域内启动新的协程任务
            val processor = videoProcessor ?: run { // 获取视频处理器实例
                addLog("❌ 视频处理器未初始化") // 如果处理器为空，记录错误日志
                return@launch // 退出协程
            } // 结束处理器获取

            _uiState.update { // 更新UI状态
                it.copy( // 复制当前状态并修改以下字段
                    videoProcessingState = VideoProcessingState.Processing, // 设置视频处理状态为处理中
                    videoProgress = 0f, // 重置进度为0
                    videoProcessedCount = 0, // 重置已处理帧数为0
                    videoProcessedFrames = emptyList(), // 清空已处理帧结果
                    outputVideoUri = null // 清空输出视频URI
                ) // 结束copy
            } // 结束_uiState.update
            addLog("🎬 开始视频人脸识别...") // 记录开始视频人脸识别的日志

            try { // 尝试执行视频处理流程
                val processedFrames = mutableListOf<VideoProcessor.ProcessedFrame>() // 创建可变列表存储处理后的帧数据
                val frameResults = mutableListOf<VideoFrameResult>() // 创建可变列表存储帧识别结果
                var lastUiUpdateCount = 0 // 记录上次UI更新时的帧数，用于节流
                val uiUpdateInterval = 5 // UI更新间隔，每处理5帧更新一次UI

                processor.processVideoFrames( // 调用视频处理器的帧处理方法
                    context = appContext, // 传入应用上下文
                    videoUri = currentState.videoUri, // 传入视频URI
                    templates = templates, // 传入人脸模板数据
                    threshold = currentState.videoThreshold, // 传入视频识别阈值
                    detectionThreshold = currentState.videoDetectionThreshold, // 传入视频检测阈值
                    onProgress = { progress -> // 进度回调函数
                        if (!coroutineContext.isActive) return@processVideoFrames // 如果协程已取消，直接返回停止处理
                        val count = progress.frameIndex + 1 // 计算当前已处理的帧数（索引从0开始，所以+1）
                        if (count - lastUiUpdateCount >= uiUpdateInterval) { // 如果距离上次UI更新已超过间隔
                            lastUiUpdateCount = count // 更新上次UI更新的帧数
                            _uiState.update { // 更新UI状态
                                it.copy(videoProcessedCount = count) // 更新已处理帧数
                            } // 结束_uiState.update
                        } // 结束UI更新节流判断
                    } // 结束onProgress回调
                ).collect { processedFrame -> // 收集Flow中的每个处理后的帧
                    if (!coroutineContext.isActive) return@collect // 如果协程已取消，直接返回停止收集

                    processedFrames.add(processedFrame) // 将处理后的帧添加到帧列表
                    frameResults.add( // 将帧结果添加到结果列表
                        VideoFrameResult( // 创建视频帧结果
                            frameIndex = processedFrame.frameIndex, // 设置帧索引
                            detections = processedFrame.detections, // 设置该帧的检测结果
                            names = processedFrame.names // 设置该帧的识别名称
                        ) // 结束VideoFrameResult创建
                    ) // 结束frameResults.add

                    val count = processedFrames.size // 获取当前已处理的总帧数
                    if (count - lastUiUpdateCount >= uiUpdateInterval) { // 如果距离上次UI更新已超过间隔
                        lastUiUpdateCount = count // 更新上次UI更新的帧数
                        _uiState.update { // 更新UI状态
                            it.copy( // 复制当前状态并修改以下字段
                                videoProcessedCount = count, // 更新已处理帧数
                                videoProcessedFrames = frameResults.toList(), // 更新已处理帧结果列表
                                videoProgress = processedFrame.presentationTimeUs.toFloat() / // 计算进度：当前帧时间戳
                                    ((currentState.videoInfo?.durationMs ?: 1L) * 1000).coerceAtLeast(1) // 除以视频总时长（微秒），最小为1防止除零
                            ) // 结束copy
                        } // 结束_uiState.update
                    } // 结束UI更新节流判断
                } // 结束Flow的collect

                if (!coroutineContext.isActive) return@launch // 如果协程已取消，直接返回

                _uiState.update { // 更新UI状态
                    it.copy( // 复制当前状态并修改以下字段
                        videoProcessedCount = processedFrames.size, // 更新已处理帧数为最终总数
                        videoProcessedFrames = frameResults.toList(), // 更新已处理帧结果为最终完整列表
                        videoProgress = 1f // 设置进度为100%
                    ) // 结束copy
                } // 结束_uiState.update

                addLog("✅ 帧处理完成，共 ${processedFrames.size} 帧，开始编码视频...") // 记录帧处理完成的日志

                _uiState.update { // 更新UI状态
                    it.copy( // 复制当前状态
                        videoProcessingState = VideoProcessingState.Encoding("正在编码输出视频...") // 设置状态为编码中
                    ) // 结束copy
                } // 结束_uiState.update

                val outputFile = File( // 创建输出视频文件对象
                    appContext.cacheDir, // 使用应用缓存目录
                    "FaceFound_video_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.mp4" // 生成带时间戳的MP4文件名
                ) // 结束File创建

                val videoInfo = currentState.videoInfo // 获取视频元信息
                val outWidth = videoInfo?.width ?: processedFrames.firstOrNull()?.bitmap?.width ?: 1920 // 确定输出视频宽度，依次尝试视频信息、首帧宽度、默认1920
                val outHeight = videoInfo?.height ?: processedFrames.firstOrNull()?.bitmap?.height ?: 1080 // 确定输出视频高度，依次尝试视频信息、首帧高度、默认1080

                val resultFile = processor.encodeToVideo( // 调用视频处理器将处理后的帧编码为视频文件
                    frames = processedFrames, // 传入处理后的帧列表
                    outputFile = outputFile, // 传入输出文件路径
                    width = outWidth, // 传入输出视频宽度
                    height = outHeight, // 传入输出视频高度
                    onProgress = { progress -> // 编码进度回调函数
                        _uiState.update { // 更新UI状态
                            it.copy( // 复制当前状态
                                videoProgress = progress, // 更新编码进度
                                videoProcessingState = VideoProcessingState.Encoding("编码中... ${(progress * 100).toInt()}%") // 更新编码状态消息显示百分比
                            ) // 结束copy
                        } // 结束_uiState.update
                    } // 结束onProgress回调
                ) // 结束encodeToVideo调用

                // Copy to shared storage // 注释说明下方代码将视频复制到共享存储
                val savedUri = saveVideoToGallery(appContext, resultFile) // 将编码后的视频文件保存到系统相册

                _uiState.update { // 更新UI状态
                    it.copy( // 复制当前状态并修改以下字段
                        videoProcessingState = VideoProcessingState.Completed, // 设置视频处理状态为完成
                        videoProgress = 1f, // 设置进度为100%
                        videoProcessedCount = processedFrames.size, // 更新已处理帧数为最终总数
                        videoProcessedFrames = frameResults.toList(), // 更新已处理帧结果为最终完整列表
                        outputVideoUri = savedUri // 设置输出视频的URI
                    ) // 结束copy
                } // 结束_uiState.update
                addLog("✅ 视频识别完成! 已保存到相册") // 记录视频识别完成的日志

            } catch (e: Exception) { // 捕获视频处理过程中的异常
                if (!coroutineContext.isActive) return@launch // 如果协程已取消，直接返回
                Log.e(TAG, "视频处理失败", e) // 输出视频处理失败的错误日志到Logcat
                _uiState.update { // 更新UI状态
                    it.copy( // 复制当前状态
                        videoProcessingState = VideoProcessingState.Error(e.message ?: "未知错误") // 设置视频处理状态为错误，附带错误消息
                    ) // 结束copy
                } // 结束_uiState.update
                addLog("❌ 视频处理失败: ${e.message}") // 记录视频处理失败的日志
            } // 结束try-catch
        } // 结束viewModelScope.launch协程
    } // 结束startVideoProcessing函数

    fun cancelVideoProcessing() { // 取消视频处理函数
        videoJob?.cancel() // 取消正在执行的视频处理协程
        videoJob = null // 将视频任务引用置空
        _uiState.update { // 更新UI状态
            it.copy( // 复制当前状态
                videoProcessingState = VideoProcessingState.Idle, // 重置视频处理状态为空闲
                videoProgress = 0f // 重置进度为0
            ) // 结束copy
        } // 结束_uiState.update
        addLog("⏹️ 已取消视频处理") // 记录取消视频处理的日志
    } // 结束cancelVideoProcessing函数

    fun saveVideoResult() { // 保存视频结果函数
        val outputUri = _uiState.value.outputVideoUri // 获取输出视频的URI
        if (outputUri != null) { // 如果输出视频URI不为空（视频已保存）
            addLog("💾 视频已保存: $outputUri") // 记录视频已保存的日志
            _uiState.update { it.copy(statusMessage = "视频已保存到相册") } // 更新状态消息为保存成功
            return // 提前返回
        } // 结束URI检查

        addLog("⚠️ 没有可保存的视频结果") // 记录没有可保存视频结果的日志
    } // 结束saveVideoResult函数

    private fun saveVideoToGallery(context: Context, videoFile: File): Uri? { // 将视频文件保存到系统相册的私有函数
        return try { // 尝试执行保存操作并返回结果
            val fileName = videoFile.name // 获取视频文件的名称
            val resolver = context.contentResolver // 获取内容解析器
            val contentValues = android.content.ContentValues().apply { // 创建ContentValues用于插入MediaStore记录
                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, fileName) // 设置视频文件显示名称
                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4") // 设置MIME类型为MP4视频
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // 如果Android版本 >= 10
                    put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/FaceFound") // 设置相对路径为Movies/FaceFound
                    put(android.provider.MediaStore.Video.Media.IS_PENDING, 1) // 标记为待处理状态
                } // 结束Android Q以上的额外设置
            } // 结束ContentValues创建

            val uri = resolver.insert( // 在MediaStore中插入新记录
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, // 使用外部存储的视频MediaStore URI
                contentValues // 传入ContentValues
            ) // 结束resolver.insert

            if (uri != null) { // 如果插入成功获取到了URI
                resolver.openOutputStream(uri)?.use { out -> // 打开输出流
                    videoFile.inputStream().use { input -> // 打开视频文件的输入流
                        input.copyTo(out) // 将视频文件内容复制到输出流
                    } // 结束输入流使用
                } // 结束输出流使用

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // 如果Android版本 >= 10
                    contentValues.clear() // 清空之前的ContentValues
                    contentValues.put(android.provider.MediaStore.Video.Media.IS_PENDING, 0) // 将待处理标记设为0
                    resolver.update(uri, contentValues, null, null) // 更新MediaStore记录，解除待处理状态
                } // 结束Android Q以上的状态更新

                addLog("💾 视频已保存到 Movies/FaceFound: $fileName") // 记录视频保存成功的日志
                uri // 返回保存后的URI
            } else { // 如果URI插入失败
                null // 返回null
            } // 结束URI有效性检查
        } catch (e: Exception) { // 捕获保存过程中的异常
            Log.e(TAG, "保存视频到相册失败: ${e.message}") // 输出保存失败的错误日志到Logcat
            addLog("❌ 保存视频失败: ${e.message}") // 记录保存视频失败的日志
            null // 返回null表示保存失败
        } // 结束try-catch
    } // 结束saveVideoToGallery函数

    // --- 辅助函数 --- // 分隔注释，标记下方为辅助工具函数

    private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = // 从URI加载位图的挂起函数，返回可空的Bitmap
        withContext(Dispatchers.IO) { // 在IO线程中执行文件读取操作
            try { // 尝试执行加载操作
                context.contentResolver.openInputStream(uri)?.use { // 通过内容解析器打开URI的输入流
                    BitmapFactory.decodeStream(it) // 从输入流中解码生成位图
                } // 结束输入流使用
            } catch (e: Exception) { // 捕获加载过程中的异常
                Log.e(TAG, "加载图片失败: ${e.message}") // 输出加载图片失败的错误日志
                null // 返回null表示加载失败
            } // 结束try-catch
        } // 结束withContext IO线程切换

    private fun addLog(message: String) { // 添加日志的私有函数，将消息附加时间戳后添加到日志列表
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) // 获取当前时间并格式化为"时:分:秒"格式
        _uiState.update { // 更新UI状态
            val newLogs = (it.logs + "[$time] $message").takeLast(100) // 将新日志追加到列表末尾，并只保留最近100条
            it.copy(logs = newLogs) // 用新的日志列表更新状态
        } // 结束_uiState.update
    } // 结束addLog函数

    private fun recycleResultBitmap() { // 回收结果位图的私有函数，安全地释放位图内存
        val bitmap = _uiState.value.resultBitmap // 获取当前状态中的结果位图引用
        // 先将状态中的引用置空，避免 UI 访问已回收的 bitmap // 注释说明先置空引用的原因
        _uiState.update { it.copy(resultBitmap = null) } // 将状态中的结果位图引用设为null
        // 然后回收 bitmap // 注释说明随后回收位图
        bitmap?.recycle() // 回收位图，释放其占用的内存
    } // 结束recycleResultBitmap函数

    override fun onCleared() { // 重写ViewModel清除回调，在ViewModel被销毁时调用
        super.onCleared() // 调用父类的onCleared方法
        videoJob?.cancel() // 取消正在执行的视频处理协程
        videoJob = null // 将视频任务引用置空
        try { // 尝试执行清理操作
            faceRecognizer?.close() // 关闭人脸识别器，释放ONNX运行时资源
        } catch (_: Exception) {} // 忽略关闭过程中可能发生的异常
    } // 结束onCleared函数
} // 结束FaceRecognitionViewModel类
