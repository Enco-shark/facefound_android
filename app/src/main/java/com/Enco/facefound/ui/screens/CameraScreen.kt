package com.Enco.facefound.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.Enco.facefound.ml.OnnxFaceRecognition
import com.Enco.facefound.ui.viewmodel.FaceRecognitionViewModel
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "CameraScreen"

/**
 * 实时摄像头人脸识别主界面
 * 包含摄像头预览、人脸叠加层和底部状态栏
 */
@Composable
fun CameraScreen(
    viewModel: FaceRecognitionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 检查摄像头权限
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // 离开页面时清除识别结果
    DisposableEffect(Unit) {
        onDispose { viewModel.clearCameraResults() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("实时识别") },
            navigationIcon = {
                IconButton(onClick = { viewModel.navigateTo(FaceRecognitionViewModel.Screen.Main) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (!hasCameraPermission) {
            CameraPermissionRequest(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                FaceOverlay(
                    detections = uiState.cameraDetections,
                    names = uiState.cameraNames,
                    imageWidth = uiState.cameraImageWidth,
                    imageHeight = uiState.cameraImageHeight,
                    modifier = Modifier.fillMaxSize()
                )
                CameraStatusBar(
                    faceCount = uiState.cameraDetections.size,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                )
            }
        }
    }
}

// 权限请求界面
@Composable
private fun CameraPermissionRequest(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("需要摄像头权限", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("实时人脸识别需要使用摄像头，请授予摄像头权限",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) { Text("授予权限") }
    }
}

/**
 * CameraX 摄像头预览 + 逐帧分析
 * 使用 ImageAnalysis 逐帧提取图像，通过 ViewModel 执行人脸检测+识别
 */
@Composable
private fun CameraPreview(
    viewModel: FaceRecognitionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 单线程执行器用于帧分析回调
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    // 协程作用域用于异步 ONNX 推理
    val analysisScope = remember { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    // 节流标志：同一时刻只处理一帧，避免积压
    val isAnalyzing = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            analysisScope.cancel()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // 预览用例
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // 图像分析用例：640x480 分析分辨率，只保留最新帧
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            // 节流：上一帧未处理完则跳过
                            if (!isAnalyzing.compareAndSet(false, true)) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            analysisScope.launch {
                                try {
                                    val bitmap = imageProxy.toBitmap()
                                    if (bitmap != null) {
                                        viewModel.analyzeCameraFrame(bitmap)
                                        bitmap.recycle()
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "帧分析失败: ${e.message}", e)
                                } finally {
                                    isAnalyzing.set(false)
                                    imageProxy.close()
                                }
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "摄像头绑定失败: ${e.message}", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

/**
 * 人脸检测叠加层
 * 将检测坐标从分析分辨率缩放到显示分辨率后绘制边界框、关键点和姓名
 */
@Composable
private fun FaceOverlay(
    detections: List<OnnxFaceRecognition.FaceDetection>,
    names: List<String>,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier
) {
    if (detections.isEmpty() || imageWidth == 0 || imageHeight == 0) return

    Canvas(modifier = modifier) {
        val scaleX = size.width / imageWidth.toFloat()
        val scaleY = size.height / imageHeight.toFloat()

        // 边界框画笔：绿色描边
        val boxPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GREEN
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = (size.width / 200f).coerceAtLeast(3f)
        }
        // 文字画笔：绿色粗体带阴影
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GREEN
            textSize = (size.width / 40f).coerceAtLeast(24f)
            isFakeBoldText = true
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
        }
        // 背景画笔：半透明黑色
        val bgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(160, 0, 0, 0)
            style = android.graphics.Paint.Style.FILL
        }
        // 关键点画笔：黄色填充
        val landmarkPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.YELLOW
            style = android.graphics.Paint.Style.FILL
        }

        val canvas = drawContext.canvas.nativeCanvas

        detections.forEachIndexed { index, detection ->
            val left = detection.rect.left * scaleX
            val top = detection.rect.top * scaleY
            val right = detection.rect.right * scaleX
            val bottom = detection.rect.bottom * scaleY

            // 绘制边界框
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // 绘制 5 个关键点
            detection.landmarks.forEach { (lx, ly) ->
                canvas.drawCircle(lx * scaleX, ly * scaleY,
                    (size.width / 400f).coerceAtLeast(3f), landmarkPaint)
            }

            // 绘制姓名标签
            val name = names.getOrElse(index) { "Face" }
            val confText = "%.0f%%".format(detection.confidence * 100)
            val label = "$name ($confText)"
            val textWidth = textPaint.measureText(label)
            val textHeight = textPaint.textSize
            val padding = 6f

            // 优先放在边界框上方，空间不足时放下方
            val labelTop = if (top - textHeight - padding * 2 >= 0)
                top - textHeight - padding * 2 else bottom

            canvas.drawRect(left, labelTop, left + textWidth + padding * 2,
                labelTop + textHeight + padding * 2, bgPaint)
            canvas.drawText(label, left + padding, labelTop + textHeight + padding / 2, textPaint)
        }
    }
}

// 底部状态栏：显示检测到的人脸数量
@Composable
private fun CameraStatusBar(faceCount: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (faceCount > 0) "检测到 $faceCount 张人脸" else "未检测到人脸",
                style = MaterialTheme.typography.bodyMedium,
                color = if (faceCount > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("实时识别中", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * ImageProxy 转 Bitmap
 * YUV_420_888 → NV21 → JPEG → Bitmap
 * 正确处理 pixelStride: 当 stride=2 时，V/U buffer 包含交错数据
 */
private fun ImageProxy.toBitmap(): Bitmap? {
    try {
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val ySize = yBuffer.remaining()
        val pixelStride = vPlane.pixelStride

        // NV21: Y 平面 + 交错的 VU 数据
        val nv21 = ByteArray(ySize + width * height / 2)

        // 逐行复制 Y（处理 rowStride > width 的情况）
        val yRowStride = yPlane.rowStride
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            yBuffer.get(nv21, row * width, width)
        }

        // 复制 VU 交错数据
        val vuRowStride = vPlane.rowStride
        val vuOffset = ySize
        if (pixelStride == 2) {
            // V buffer 已是 [V0,U0,V1,U1,...] 交错格式
            for (row in 0 until height / 2) {
                vBuffer.position(row * vuRowStride)
                vBuffer.get(nv21, vuOffset + row * width, width)
            }
        } else {
            // V/U 分离，需手动交错
            for (row in 0 until height / 2) {
                val destRow = vuOffset + row * width
                vBuffer.position(row * vuRowStride)
                uBuffer.position(row * uPlane.rowStride)
                for (col in 0 until width / 2) {
                    nv21[destRow + col * 2] = vBuffer.get()
                    nv21[destRow + col * 2 + 1] = uBuffer.get()
                }
            }
        }

        // NV21 → JPEG → Bitmap
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 85, out)
        val bitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size()) ?: return null

        // 处理摄像头旋转
        val rotation = imageInfo.rotationDegrees
        return if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                bitmap.recycle()
            }
        } else bitmap
    } catch (e: Exception) {
        Log.e(TAG, "ImageProxy→Bitmap 失败: ${e.message}", e)
        return null
    }
}
