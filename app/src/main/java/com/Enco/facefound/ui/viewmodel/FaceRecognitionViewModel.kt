package com.Enco.facefound.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.Enco.facefound.ml.OnnxFaceRecognition
import com.Enco.facefound.util.NpzParser
import com.Enco.facefound.util.TemplateRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 人脸识别 ViewModel
 * 稳定性优先，带完整异常处理和性能监控
 */
class FaceRecognitionViewModel(application: Application) : AndroidViewModel(application) {

    // --- 状态数据类 ---

    data class UiState(
        val statusMessage: String = "准备就绪",
        val isReady: Boolean = false,
        val isModelLoaded: Boolean = false,
        val inputImageUri: Uri? = null,
        val resultBitmap: Bitmap? = null,
        val templateName: String? = null,
        val templateUri: Uri? = null,
        val threshold: Float = 0.45f,
        val isProcessing: Boolean = false,
        val logs: List<String> = emptyList(),
        val detectedFaces: List<OnnxFaceRecognition.FaceDetection> = emptyList(),
        val recognitionResults: List<OnnxFaceRecognition.RecognitionResult> = emptyList(),
        val processingTimeMs: Long = 0,
        val isDarkTheme: Boolean = false,
        val currentScreen: Screen = Screen.Main,
        val templateList: List<TemplateItem> = emptyList(),
        val recognitionHistory: List<RecognitionHistoryItem> = emptyList(),
        val isCameraEnabled: Boolean = true,
        val imageDownsample: Boolean = true
    )

    enum class Screen {
        Main, Camera, Templates, History, Settings
    }

    data class TemplateItem(
        val name: String,
        val embedding: FloatArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as TemplateItem
            if (name != other.name) return false
            if (!embedding.contentEquals(other.embedding)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + embedding.contentHashCode()
            return result
        }
    }

    data class RecognitionHistoryItem(
        val id: Long = System.currentTimeMillis(),
        val timestamp: Date = Date(),
        val imagePath: String? = null,
        val recognizedNames: List<String> = emptyList(),
        val processingTimeMs: Long = 0
    )

    // --- 状态管理 ---

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var faceRecognizer: OnnxFaceRecognition? = null
    private var templates: Map<String, FloatArray> = emptyMap()
    private val templateRepo by lazy { TemplateRepository(application) }

    companion object {
        private const val TAG = "FaceRecognitionVM"
    }

    // --- 初始化 ---

    fun initialize() {
        if (faceRecognizer?.isLoaded == true) return
        val context = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "正在加载模型...") }

            val result = runCatching {
                withContext(Dispatchers.IO) {
                    OnnxFaceRecognition(context)
                }
            }

            val newRecognizer = result.getOrNull()
            if (newRecognizer != null) {
                faceRecognizer?.close()
                faceRecognizer = newRecognizer
            }
            val isLoaded = faceRecognizer?.isLoaded ?: false

            _uiState.update {
                it.copy(
                    isModelLoaded = isLoaded,
                    isReady = isLoaded,
                    statusMessage = if (isLoaded) "模型已加载" else "模型加载失败"
                )
            }

            if (isLoaded) {
                addLog("✅ 模型加载成功")

                val savedTemplates = templateRepo.loadAll()
                val validTemplates = savedTemplates.filter { it.value.size == OnnxFaceRecognition.EMBEDDING_DIM }
                if (validTemplates.size < savedTemplates.size) {
                    val invalidCount = savedTemplates.size - validTemplates.size
                    addLog("⚠️ 过滤掉 $invalidCount 个无效模板（维度不匹配，应为${OnnxFaceRecognition.EMBEDDING_DIM}维）")
                    templateRepo.save(validTemplates)
                }
                if (validTemplates.isNotEmpty()) {
                    templates = validTemplates
                    val templateList = validTemplates.map { (name, emb) -> TemplateItem(name, emb) }
                    _uiState.update { it.copy(templateList = templateList) }
                    addLog("📂 已自动加载 ${validTemplates.size} 个模板，无需二次导入")
                } else {
                    addLog("💡 请导入 NPZ 模板文件，导入后将自动保存")
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "未知错误"
                addLog("❌ 模型加载失败: $error")
            }
        }
    }

    // --- 屏幕导航 ---

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    // --- 主题切换 ---

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    // --- 设置变更 ---

    fun updateThreshold(value: Float) {
        _uiState.update { it.copy(threshold = value) }
    }

    fun setImageDownsample(enabled: Boolean) {
        _uiState.update { it.copy(imageDownsample = enabled) }
    }

    fun setCameraEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isCameraEnabled = enabled) }
    }

    // --- 图片操作 ---

    fun setInputImage(uri: Uri) {
        val oldBitmap = _uiState.value.resultBitmap
        _uiState.update {
            it.copy(
                inputImageUri = uri,
                resultBitmap = null,
                statusMessage = "图片已加载"
            )
        }
        oldBitmap?.recycle()
        addLog("📷 加载图片: ${uri.lastPathSegment}")
    }

    // --- 模板管理 ---

    fun setTemplate(uri: Uri) {
        val appContext = getApplication<Application>()
        _uiState.update {
            it.copy(
                templateUri = uri,
                templateName = uri.lastPathSegment
            )
        }
        addLog("📂 正在解析模板: ${uri.lastPathSegment}")

        viewModelScope.launch {
            val parsedTemplates = NpzParser.parseFromUri(appContext, uri)

            val templateList = parsedTemplates.map { (name, embedding) ->
                TemplateItem(name, embedding)
            }

            if (parsedTemplates.isNotEmpty()) {
                val mergedTemplates = templates.toMutableMap().apply {
                    putAll(parsedTemplates)
                }
                templates = mergedTemplates
                _uiState.update { state ->
                    state.copy(
                        templateName = "${uri.lastPathSegment} (${parsedTemplates.size}人)",
                        templateList = templates.map { (name, emb) -> TemplateItem(name, emb) }
                    )
                }
                viewModelScope.launch(Dispatchers.IO) { templateRepo.save(templates) }
                addLog("✅ 模板加载并保存成功: ${parsedTemplates.size} 个新模板，当前共 ${templates.size} 个模板")
            } else {
                _uiState.update { state ->
                    state.copy(
                        templateName = "${uri.lastPathSegment} (解析失败)"
                    )
                }
                addLog("❌ 模板解析失败，请检查文件格式")
            }
        }
    }

    fun removeTemplate(name: String) {
        templates = templates.filter { it.key != name }
        _uiState.update { state ->
            state.copy(
                templateList = state.templateList.filter { it.name != name }
            )
        }
        viewModelScope.launch(Dispatchers.IO) { templateRepo.save(templates) }
        addLog("❌ 已删除模板: $name")
    }

    fun renameTemplate(oldName: String, newName: String) {
        val embedding = templates[oldName] ?: return
        val newTemplates = templates.toMutableMap().apply {
            remove(oldName)
            put(newName, embedding)
        }
        templates = newTemplates
        _uiState.update { state ->
            state.copy(
                templateList = state.templateList.map {
                    if (it.name == oldName) it.copy(name = newName) else it
                }
            )
        }
        viewModelScope.launch(Dispatchers.IO) { templateRepo.save(templates) }
        addLog("✏️ 模板已重命名: $oldName → $newName")
    }

    fun addTemplateFromFace(
        name: String,
        faceBitmap: Bitmap
    ) {
        viewModelScope.launch {
            try {
                val embedding = faceRecognizer?.extractEmbedding(faceBitmap) ?: return@launch
                templates = templates.toMutableMap().apply { put(name, embedding) }
                _uiState.update { state ->
                    state.copy(
                        templateList = state.templateList + TemplateItem(name, embedding)
                    )
                }
                withContext(Dispatchers.IO) { templateRepo.save(templates) }
                addLog("✅ 已添加新模板: $name")
            } catch (e: Exception) {
                addLog("❌ 添加模板失败: ${e.message}")
            }
        }
    }

    // --- 识别功能 ---

    fun startRecognition() {
        val appContext = getApplication<Application>()
        val currentState = _uiState.value

        if (!currentState.isModelLoaded) {
            _uiState.update { it.copy(statusMessage = "模型未加载") }
            addLog("⚠️ 请先加载模型")
            return
        }

        if (currentState.inputImageUri == null) {
            _uiState.update { it.copy(statusMessage = "请先选择图片") }
            addLog("⚠️ 请先选择图片")
            return
        }

        viewModelScope.launch {
            val totalStartTime = System.currentTimeMillis()

            // 先回收旧的 bitmap
            val oldBitmap = _uiState.value.resultBitmap
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    statusMessage = "正在识别...",
                    resultBitmap = null
                )
            }
            oldBitmap?.recycle()

            var sourceBitmap: Bitmap? = null
            var sourceBitmapTransferred = false

            try {
                sourceBitmap = loadBitmapFromUri(appContext, currentState.inputImageUri)
                if (sourceBitmap == null) {
                    throw Exception("无法加载图片，可能格式不支持")
                }

                val maxDimension = if (currentState.imageDownsample) 4096 else 8192
                if (sourceBitmap.width > maxDimension || sourceBitmap.height > maxDimension) {
                    val scale = maxDimension.toFloat() / maxOf(sourceBitmap.width, sourceBitmap.height)
                    val newWidth = (sourceBitmap.width * scale).toInt()
                    val newHeight = (sourceBitmap.height * scale).toInt()
                    val scaled = Bitmap.createScaledBitmap(sourceBitmap, newWidth, newHeight, true)
                    sourceBitmap.recycle()
                    sourceBitmap = scaled
                    addLog("📐 图片已缩放至 ${newWidth}x${newHeight}")
                }

                addLog("🔍 开始人脸检测...")

                val detections = faceRecognizer?.detectFaces(sourceBitmap) ?: emptyList()
                _uiState.update { it.copy(detectedFaces = detections) }
                addLog("✅ 检测到 ${detections.size} 张人脸")

                if (detections.isEmpty()) {
                    _uiState.update { state ->
                        state.copy(
                            isProcessing = false,
                            statusMessage = "未检测到人脸",
                            resultBitmap = sourceBitmap
                        )
                    }
                    sourceBitmapTransferred = true
                    addLog("⚠️ 未检测到人脸")
                    return@launch
                }

                val results = mutableListOf<OnnxFaceRecognition.RecognitionResult>()
                val recognizedNames = mutableListOf<String>()

                detections.forEachIndexed { index, detection ->
                    var singleFaceBitmap: Bitmap? = null
                    try {
                        addLog("🔍 处理人脸 ${index + 1}/${detections.size}...")

                        singleFaceBitmap = faceRecognizer?.alignFace(sourceBitmap, detection)
                            ?: run {
                                addLog("⚠️ 人脸 ${index + 1} alignFace 返回 null，尝试 bbox 裁剪")
                                val left = detection.rect.left.coerceIn(0, sourceBitmap.width - 1)
                                val top = detection.rect.top.coerceIn(0, sourceBitmap.height - 1)
                                val right = detection.rect.right.coerceIn(left + 1, sourceBitmap.width)
                                val bottom = detection.rect.bottom.coerceIn(top + 1, sourceBitmap.height)
                                val width = right - left
                                val height = bottom - top
                                if (width <= 0 || height <= 0) {
                                    null
                                } else {
                                    Bitmap.createBitmap(sourceBitmap, left, top, width, height)
                                }
                            }

                        if (singleFaceBitmap == null) {
                            addLog("⚠️ 人脸 ${index + 1} 裁剪区域无效，跳过")
                            results.add(OnnxFaceRecognition.RecognitionResult("UNKNOWN", 0f))
                            recognizedNames.add("UNKNOWN")
                            return@forEachIndexed
                        }

                        addLog("📐 人脸 ${index + 1} 对齐完成: ${singleFaceBitmap.width}x${singleFaceBitmap.height}")

                        val result = faceRecognizer?.recognizeFace(
                            singleFaceBitmap,
                            templates,
                            currentState.threshold
                        ) ?: OnnxFaceRecognition.RecognitionResult("UNKNOWN", 0f)

                        results.add(result)
                        recognizedNames.add(result.name)
                        addLog("✅ 人脸 ${index + 1}: ${result.name} (${String.format("%.2f", result.confidence)})")

                    } catch (e: OutOfMemoryError) {
                        Log.e(TAG, "人脸 ${index + 1} OOM", e)
                        addLog("❌ 人脸 ${index + 1} 内存不足: ${e.message}")
                        System.gc()
                        results.add(OnnxFaceRecognition.RecognitionResult("UNKNOWN", 0f))
                        recognizedNames.add("UNKNOWN")
                    } catch (e: Exception) {
                        Log.e(TAG, "人脸 ${index + 1} 处理失败", e)
                        addLog("❌ 人脸 ${index + 1} 处理失败: ${e.javaClass.simpleName}: ${e.message}")
                        results.add(OnnxFaceRecognition.RecognitionResult("UNKNOWN", 0f))
                        recognizedNames.add("UNKNOWN")
                    } finally {
                        singleFaceBitmap?.recycle()
                    }
                }

                val resultBitmap = faceRecognizer?.drawDetections(sourceBitmap, detections, recognizedNames)
                val totalTime = System.currentTimeMillis() - totalStartTime

                val historyItem = RecognitionHistoryItem(
                    timestamp = Date(),
                    recognizedNames = recognizedNames,
                    processingTimeMs = totalTime
                )

                _uiState.update { state ->
                    state.copy(
                        recognitionResults = results,
                        resultBitmap = resultBitmap,
                        isProcessing = false,
                        statusMessage = "识别完成: ${results.size} 张人脸 (${totalTime}ms)",
                        processingTimeMs = totalTime,
                        recognitionHistory = listOf(historyItem) + state.recognitionHistory.take(49)
                    )
                }

                addLog("✅ 识别完成，总耗时 ${totalTime}ms")

            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "内存不足", e)
                _uiState.update { state ->
                    state.copy(
                        isProcessing = false,
                        statusMessage = "内存不足，请尝试更小的图片"
                    )
                }
                addLog("❌ 内存不足: ${e.message}")
                System.gc()
            } catch (e: Exception) {
                Log.e(TAG, "识别失败", e)
                _uiState.update { state ->
                    state.copy(
                        isProcessing = false,
                        statusMessage = "识别失败: ${e.message}"
                    )
                }
                addLog("❌ 识别失败: ${e.javaClass.simpleName}: ${e.message}")
            } catch (e: Throwable) {
                Log.e(TAG, "识别严重错误", e)
                _uiState.update { state ->
                    state.copy(
                        isProcessing = false,
                        statusMessage = "严重错误: ${e.message}"
                    )
                }
                addLog("❌ 严重错误: ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                if (sourceBitmap != null && !sourceBitmapTransferred) {
                    sourceBitmap.recycle()
                }
            }
        }
    }

    fun saveResultImage() {
        val appContext = getApplication<Application>()
        val bitmap = _uiState.value.resultBitmap?.copy(Bitmap.Config.ARGB_8888, false) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = "FaceFound_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
                val resolver = appContext.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FaceFound")
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    addLog("💾 图片已保存到相册: $fileName")
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(statusMessage = "图片已保存到相册") }
                    }
                } else {
                    addLog("❌ 无法创建相册条目")
                }
            } catch (e: Exception) {
                addLog("❌ 保存图片失败: ${e.message}")
            } finally {
                bitmap.recycle()
            }
        }
    }

    fun clearHistory() {
        _uiState.update {
            it.copy(recognitionHistory = emptyList())
        }
        addLog("🗑️ 识别历史已清空")
    }

    // --- 辅助函数 ---

    private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载图片失败: ${e.message}")
                null
            }
        }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _uiState.update {
            val newLogs = (it.logs + "[$time] $message").takeLast(100)
            it.copy(logs = newLogs)
        }
    }

    private fun recycleResultBitmap() {
        val bitmap = _uiState.value.resultBitmap
        // 先将状态中的引用置空，避免 UI 访问已回收的 bitmap
        _uiState.update { it.copy(resultBitmap = null) }
        // 然后回收 bitmap
        bitmap?.recycle()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            faceRecognizer?.close()
        } catch (_: Exception) {}
    }
}
