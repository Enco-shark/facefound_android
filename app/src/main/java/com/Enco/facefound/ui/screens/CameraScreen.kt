package com.Enco.facefound.ui.screens // 声明当前文件所属的包路径，用于组织和引用项目中的类

// 导入 Android 权限常量，用于请求摄像头权限
import android.Manifest
// 导入 PackageManager 类，用于检查权限授予状态
import android.content.pm.PackageManager
// 导入 Bitmap 类，用于图像的内存表示和像素操作
import android.graphics.Bitmap
// 导入 BitmapFactory 类，用于从字节数组解码 Bitmap
import android.graphics.BitmapFactory
// 导入 ImageFormat 类，用于指定图像格式常量（如 NV21）
import android.graphics.ImageFormat
// 导入 Matrix 类，用于二维图形变换（旋转、缩放等）
import android.graphics.Matrix
// 导入 Rect 类，用于表示矩形区域（JPEG 压缩裁剪区域）
import android.graphics.Rect
// 导入 YuvImage 类，用于 YUV 格式图像的 JPEG 压缩
import android.graphics.YuvImage
// 导入 Log 类，用于在 Logcat 中输出调试和错误日志
import android.util.Log
// 导入 Size 类，用于指定 CameraX 分析目标分辨率
import android.util.Size
// 导入 rememberLauncherForActivityResult，用于创建权限请求回调
import androidx.activity.compose.rememberLauncherForActivityResult
// 导入 RequestPermission 契约，用于单个权限请求
import androidx.activity.result.contract.ActivityResultContracts
// 导入 CameraSelector，用于选择前置或后置摄像头
import androidx.camera.core.CameraSelector
// 导入 ImageAnalysis，用于逐帧图像分析
import androidx.camera.core.ImageAnalysis
// 导入 ImageProxy，用于访问摄像头帧数据
import androidx.camera.core.ImageProxy
// 导入 Preview，用于摄像头预览用例
import androidx.camera.core.Preview
// 导入 ProcessCameraProvider，用于管理摄像头生命周期
import androidx.camera.lifecycle.ProcessCameraProvider
// 导入 PreviewView，用于在 Compose 中显示摄像头预览
import androidx.camera.view.PreviewView
// 导入 Compose Canvas，用于自定义绘制
import androidx.compose.foundation.Canvas
// 导入布局相关修饰符（fillMaxSize、padding 等）
import androidx.compose.foundation.layout.*
// 导入圆角矩形形状，用于底部状态栏圆角
import androidx.compose.foundation.shape.RoundedCornerShape
// 导入 Material 图标集合对象
import androidx.compose.material.icons.Icons
// 导入返回箭头图标
import androidx.compose.material.icons.filled.ArrowBack
// 导入 Material 3 组件（TopAppBar、Button、Text 等）
import androidx.compose.material3.*
// 导入 Compose 运行时（remember、mutableStateOf、DisposableEffect 等）
import androidx.compose.runtime.*
// 导入对齐相关修饰符（Alignment.CenterVertically 等）
import androidx.compose.ui.Alignment
// 导入 Compose Modifier 基类
import androidx.compose.ui.Modifier
// 导入 nativeCanvas 扩展属性，将 Compose Canvas 转为 Android Canvas
import androidx.compose.ui.graphics.nativeCanvas
// 导入 LocalContext，用于获取当前 Android Context
import androidx.compose.ui.platform.LocalContext
// 导入 LocalLifecycleOwner，用于获取当前 LifecycleOwner 绑定 CameraX
import androidx.compose.ui.platform.LocalLifecycleOwner
// 导入 dp 单位扩展，用于 UI 尺寸定义
import androidx.compose.ui.unit.dp
// 导入 AndroidView，用于在 Compose 中嵌入传统 Android View
import androidx.compose.ui.viewinterop.AndroidView
// 导入 ContextCompat，用于兼容性权限检查
import androidx.core.content.ContextCompat
// 导入 OnnxFaceRecognition，用于人脸检测和识别
import com.Enco.facefound.ml.OnnxFaceRecognition
// 导入 FaceRecognitionViewModel，用于管理 UI 状态和业务逻辑
import com.Enco.facefound.ui.viewmodel.FaceRecognitionViewModel
// 导入协程相关工具（launch、Dispatchers、SupervisorJob 等）
import kotlinx.coroutines.*
// 导入 ByteArrayOutputStream，用于收集 JPEG 压缩数据
import java.io.ByteArrayOutputStream
// 导入 Executors，用于创建单线程执行器处理摄像头帧
import java.util.concurrent.Executors
// 导入 AtomicBoolean，用于线程安全的帧分析节流标志
import java.util.concurrent.atomic.AtomicBoolean

// 定义日志标签，用于 Logcat 过滤本类的日志输出
private const val TAG = "CameraScreen" // 摄像头屏幕日志标签

// 标记 CameraScreen 为可组合函数，实时摄像头人脸识别的顶层界面
@Composable // 声明为 Compose 可组合函数
fun CameraScreen( // 定义摄像头屏幕函数
    viewModel: FaceRecognitionViewModel, // 接收 ViewModel 参数，用于状态管理和业务逻辑
    modifier: Modifier = Modifier // 接收外部修饰符，默认为空 Modifier
) { // 函数体开始
    // 获取当前 Android Context，用于权限检查和摄像头初始化
    val context = LocalContext.current // 获取 Compose 上下文
    // 收集 ViewModel 的 UI 状态，自动响应状态变化触发重组
    val uiState by viewModel.uiState.collectAsState() // 响应式收集 UI 状态

    // 检查摄像头权限是否已授予，使用 remember 保存状态避免重复检查
    var hasCameraPermission by remember { // 可变状态，权限授予后更新
        mutableStateOf( // 创建可观察的状态
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == // 检查摄像头权限
                    PackageManager.PERMISSION_GRANTED // 判断是否已授权
        ) // 状态初始值
    } // 权限状态声明结束

    // 创建权限请求回调，用户授予权限后更新 hasCameraPermission 状态
    val permissionLauncher = rememberLauncherForActivityResult( // 创建权限请求启动器
        ActivityResultContracts.RequestPermission() // 使用单权限请求契约
    ) { granted -> // 回调：用户操作完成后
        hasCameraPermission = granted // 更新权限状态（true=授予，false=拒绝）
    } // 权限回调结束

    // 注册 DisposableEffect，在离开页面时清除摄像头识别结果
    DisposableEffect(Unit) { // 创建一次性副作用，key 为 Unit 表示只执行一次
        onDispose { // 清理回调：当 Composable 从组合树移除时执行
            viewModel.clearCameraResults() // 清除摄像头检测结果，释放状态内存
        } // onDispose 结束
    } // DisposableEffect 结束

    // 创建垂直布局容器，排列顶部栏和摄像头内容
    Column(modifier = modifier.fillMaxSize()) { // Column 填满整个可用空间
        // 渲染顶部应用栏，包含标题和返回按钮
        TopAppBar( // Material 3 顶部应用栏组件
            title = { Text("实时识别") }, // 显示页面标题
            navigationIcon = { // 导航图标区域（左侧）
                IconButton(onClick = { viewModel.navigateTo(FaceRecognitionViewModel.Screen.Main) }) { // 返回按钮点击事件，导航到主页
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回") // 显示返回箭头图标
                } // IconButton 结束
            }, // navigationIcon 结束
            colors = TopAppBarDefaults.topAppBarColors( // 设置顶部栏颜色
                containerColor = MaterialTheme.colorScheme.surface // 使用主题表面颜色作为背景
            ) // 颜色配置结束
        ) // TopAppBar 结束

        // 根据权限状态显示不同界面
        if (!hasCameraPermission) { // 如果未获得摄像头权限
            // 显示权限请求界面，引导用户授予权限
            CameraPermissionRequest( // 渲染权限请求组件
                onRequestPermission = { // 权限请求按钮点击回调
                    permissionLauncher.launch(Manifest.permission.CAMERA) // 发起摄像头权限请求
                } // onRequestPermission 回调结束
            ) // CameraPermissionRequest 调用结束
        } else { // 如果已获得摄像头权限
            // 使用 Box 叠加显示摄像头预览、人脸叠加层和状态栏
            Box(modifier = Modifier.fillMaxSize()) { // Box 填满可用空间，子项可叠加
                // 渲染摄像头预览（最底层）
                CameraPreview( // 渲染摄像头预览组件
                    viewModel = viewModel, // 传入 ViewModel 用于帧分析
                    modifier = Modifier.fillMaxSize() // 预览填满整个区域
                ) // CameraPreview 调用结束

                // 渲染人脸检测叠加层（叠加在预览之上）
                FaceOverlay( // 渲染人脸边界框和姓名标注
                    detections = uiState.cameraDetections, // 传入当前检测到的人脸列表
                    names = uiState.cameraNames, // 传入当前识别到的人名列表
                    imageWidth = uiState.cameraImageWidth, // 传入分析帧宽度，用于坐标缩放
                    imageHeight = uiState.cameraImageHeight, // 传入分析帧高度，用于坐标缩放
                    modifier = Modifier.fillMaxSize() // 叠加层填满整个区域
                ) // FaceOverlay 调用结束

                // 渲染底部状态栏（最顶层，显示检测状态）
                CameraStatusBar( // 渲染摄像头状态栏组件
                    faceCount = uiState.cameraDetections.size, // 传入检测到的人脸数量
                    modifier = Modifier // 开始链式修饰符
                        .align(Alignment.BottomCenter) // 对齐到 Box 底部居中
                        .fillMaxWidth() // 填满宽度
                ) // CameraStatusBar 调用结束
            } // Box 结束
        } // 权限判断结束
    } // Column 结束
} // CameraScreen 函数结束

// 标记 CameraPermissionRequest 为可组合函数，显示权限请求界面
@Composable // 声明为 Compose 可组合函数
private fun CameraPermissionRequest( // 定义权限请求界面函数
    onRequestPermission: () -> Unit // 接收权限请求按钮点击回调
) { // 函数体开始
    // 创建垂直居中布局，显示权限说明和请求按钮
    Column( // 创建垂直布局容器
        modifier = Modifier // 开始链式修饰符
            .fillMaxSize() // 填满整个可用空间
            .padding(32.dp), // 添加 32dp 内边距
        horizontalAlignment = Alignment.CenterHorizontally, // 水平居中对齐
        verticalArrangement = Arrangement.Center // 垂直居中排列
    ) { // Column 内容开始
        // 显示权限请求标题
        Text( // 文本组件
            "需要摄像头权限", // 标题文字
            style = MaterialTheme.typography.headlineSmall, // 使用小标题排版样式
            color = MaterialTheme.colorScheme.onSurface // 使用主题表面文字颜色
        ) // 标题文本结束
        // 标题和说明之间的间距
        Spacer(Modifier.height(16.dp)) // 添加 16dp 垂直间距
        // 显示权限请求说明
        Text( // 文本组件
            "实时人脸识别需要使用摄像头，请授予摄像头权限", // 说明文字
            style = MaterialTheme.typography.bodyMedium, // 使用中正文排版样式
            color = MaterialTheme.colorScheme.onSurfaceVariant // 使用主题表面变体文字颜色
        ) // 说明文本结束
        // 说明和按钮之间的间距
        Spacer(Modifier.height(24.dp)) // 添加 24dp 垂直间距
        // 权限请求按钮
        Button(onClick = onRequestPermission) { // 按钮点击时调用权限请求回调
            Text("授予权限") // 按钮文字
        } // Button 结束
    } // Column 结束
} // CameraPermissionRequest 函数结束

// 标记 CameraPreview 为可组合函数，显示 CameraX 实时预览并执行帧分析
@Composable // 声明为 Compose 可组合函数
private fun CameraPreview( // 定义摄像头预览函数
    viewModel: FaceRecognitionViewModel, // 接收 ViewModel，用于调用 analyzeCameraFrame
    modifier: Modifier = Modifier // 接收外部修饰符，默认为空 Modifier
) { // 函数体开始
    // 获取当前 Android Context，用于初始化 CameraProvider
    val context = LocalContext.current // 获取 Compose 上下文
    // 获取当前 LifecycleOwner，用于将 CameraX 绑定到生命周期
    val lifecycleOwner = LocalLifecycleOwner.current // 获取生命周期所有者
    // 收集 ViewModel 的 UI 状态（虽然此处未直接使用，但保持状态订阅）
    val uiState by viewModel.uiState.collectAsState() // 响应式收集 UI 状态

    // 创建单线程执行器，用于 CameraX ImageAnalysis 帧分析回调
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() } // 单线程池，确保帧按序处理
    // 创建协程作用域，用于异步执行帧分析（ONNX 推理）
    val analysisScope = remember { CoroutineScope(Dispatchers.Default + SupervisorJob()) } // Default 调度器适合 CPU 密集型任务
    // 创建线程安全的节流标志，防止帧积压（同一时刻只处理一帧）
    val isAnalyzing = remember { AtomicBoolean(false) } // 原子布尔，CAS 操作保证线程安全

    // 注册 DisposableEffect，在离开页面时清理摄像头和协程资源
    DisposableEffect(Unit) { // 创建一次性副作用
        onDispose { // 清理回调：当 Composable 从组合树移除时执行
            cameraExecutor.shutdown() // 关闭线程执行器，停止接受新任务
            analysisScope.cancel() // 取消协程作用域，终止所有正在运行的分析任务
        } // onDispose 结束
    } // DisposableEffect 结束

    // 使用 AndroidView 在 Compose 中嵌入 CameraX PreviewView
    AndroidView( // Compose 包装传统 Android View 的组件
        factory = { ctx -> // 工厂函数：首次组合时创建 View，后续重组不重复创建
            // 创建 PreviewView 实例，用于显示摄像头实时画面
            val previewView = PreviewView(ctx).apply { // 创建并配置 PreviewView
                scaleType = PreviewView.ScaleType.FILL_CENTER // 填充模式：居中填充，可能裁剪
            } // PreviewView 配置结束

            // 异步获取 CameraProvider 实例（耗时操作，在主线程回调）
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx) // 获取摄像头提供者 Future
            cameraProviderFuture.addListener({ // 添加监听器，在主线程执行
                // 获取 CameraProvider 实例（阻塞调用，但已在监听器中确保完成）
                val cameraProvider = cameraProviderFuture.get() // 获取摄像头提供者

                // 创建预览用例，将摄像头画面显示到 PreviewView
                val preview = Preview.Builder() // 创建预览构建器
                    .build() // 构建预览用例
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) } // 设置预览输出到 PreviewView

                // 创建图像分析用例，逐帧提取图像用于人脸检测
                val imageAnalysis = ImageAnalysis.Builder() // 创建图像分析构建器
                    .setTargetResolution(Size(640, 480)) // 设置目标分析分辨率（640x480，平衡精度和性能）
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 背压策略：只保留最新帧，丢弃旧帧
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) // 输出格式：YUV_420_888（兼容性最好）
                    .build() // 构建图像分析用例
                    .also { analysis -> // 配置分析器
                        // 在单线程执行器上设置帧分析回调
                        analysis.setAnalyzer(cameraExecutor) { imageProxy -> // 每收到一帧时回调
                            // 节流检查：如果上一帧仍在分析中，跳过当前帧
                            if (!isAnalyzing.compareAndSet(false, true)) { // CAS 操作：原子地将 false 改为 true
                                imageProxy.close() // 跳过帧时必须关闭 ImageProxy 释放资源
                                return@setAnalyzer // 跳过当前帧，等待下一帧
                            } // 节流检查结束

                            // 在协程中异步执行帧分析（ONNX 推理是耗时操作）
                            analysisScope.launch { // 在 Default 调度器上启动协程
                                try { // 尝试执行帧分析
                                    // 将 ImageProxy（YUV 格式）转换为 Bitmap（RGB 格式）
                                    val bitmap = imageProxy.toBitmap() // YUV→NV21→JPEG→Bitmap
                                    if (bitmap != null) { // 转换成功
                                        // 调用 ViewModel 执行人脸检测+识别
                                        viewModel.analyzeCameraFrame(bitmap) // 检测+识别，更新 UI 状态
                                        bitmap.recycle() // 释放 Bitmap 内存，避免内存泄漏
                                    } // Bitmap 空检查结束
                                } catch (e: Exception) { // 捕获分析过程中的异常
                                    // 记录错误日志，包含异常类型和消息
                                    Log.e(TAG, "帧分析失败: ${e.message}", e) // 输出错误日志到 Logcat
                                } finally { // 无论成功或失败都执行的清理代码
                                    isAnalyzing.set(false) // 重置节流标志，允许下一帧分析
                                    imageProxy.close() // 关闭 ImageProxy，释放摄像头帧资源
                                } // try-catch-finally 结束
                            } // 协程 launch 结束
                        } // setAnalyzer 回调结束
                    } // also 配置结束

                try { // 尝试绑定摄像头用例到生命周期
                    // 解除所有已绑定的用例（避免重复绑定冲突）
                    cameraProvider.unbindAll() // 解除之前的绑定
                    // 将预览和分析用例绑定到后置摄像头的生命周期
                    cameraProvider.bindToLifecycle( // 绑定用例到生命周期
                        lifecycleOwner, // 传入生命周期所有者（Activity/Fragment）
                        CameraSelector.DEFAULT_BACK_CAMERA, // 选择后置摄像头
                        preview, // 绑定预览用例
                        imageAnalysis // 绑定图像分析用例
                    ) // bindToLifecycle 结束
                } catch (e: Exception) { // 捕获绑定失败异常
                    // 记录摄像头绑定失败的错误日志
                    Log.e(TAG, "摄像头绑定失败: ${e.message}", e) // 输出错误日志到 Logcat
                } // try-catch 结束
            }, ContextCompat.getMainExecutor(ctx)) // 在主线程执行监听器回调

            // 返回创建的 PreviewView，Compose 将其添加到视图树
            previewView // 工厂函数返回值
        }, // factory 结束
        modifier = modifier // 应用外部传入的修饰符
    ) // AndroidView 结束
} // CameraPreview 函数结束

// 标记 FaceOverlay 为可组合函数，在摄像头预览上绘制人脸边界框和姓名
@Composable // 声明为 Compose 可组合函数
private fun FaceOverlay( // 定义人脸叠加层函数
    detections: List<OnnxFaceRecognition.FaceDetection>, // 传入检测到的人脸列表（包含边界框和关键点）
    names: List<String>, // 传入识别到的人名列表（与检测结果一一对应）
    imageWidth: Int, // 传入分析帧宽度（用于坐标映射）
    imageHeight: Int, // 传入分析帧高度（用于坐标映射）
    modifier: Modifier = Modifier // 接收外部修饰符，默认为空 Modifier
) { // 函数体开始
    // 如果没有检测结果或图像尺寸无效，直接返回不绘制
    if (detections.isEmpty() || imageWidth == 0 || imageHeight == 0) return // 快速退出条件

    // 使用 Compose Canvas 进行自定义绘制
    Canvas(modifier = modifier) { // Canvas 绘制区域
        // 计算坐标缩放比例：从分析分辨率映射到显示分辨率
        val scaleX = size.width / imageWidth.toFloat() // 水平缩放比例（显示宽度/分析宽度）
        val scaleY = size.height / imageHeight.toFloat() // 垂直缩放比例（显示高度/分析高度）

        // 创建边界框画笔：绿色描边矩形
        val boxPaint = android.graphics.Paint().apply { // 创建 Android 原生 Paint 对象
            color = android.graphics.Color.GREEN // 设置颜色为绿色
            style = android.graphics.Paint.Style.STROKE // 设置描边样式（不填充）
            strokeWidth = (size.width / 200f).coerceAtLeast(3f) // 设置线宽，自适应屏幕大小，最小 3px
        } // boxPaint 配置结束

        // 创建文字画笔：绿色粗体文字，带阴影
        val textPaint = android.graphics.Paint().apply { // 创建 Android 原生 Paint 对象
            color = android.graphics.Color.GREEN // 设置颜色为绿色
            textSize = (size.width / 40f).coerceAtLeast(24f) // 设置文字大小，自适应屏幕大小，最小 24px
            isFakeBoldText = true // 启用粗体效果
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK) // 添加黑色阴影，增强可读性
        } // textPaint 配置结束

        // 创建背景画笔：半透明黑色填充（用于文字背景）
        val bgPaint = android.graphics.Paint().apply { // 创建 Android 原生 Paint 对象
            color = android.graphics.Color.argb(160, 0, 0, 0) // 设置颜色为半透明黑色（alpha=160/255）
            style = android.graphics.Paint.Style.FILL // 设置填充样式
        } // bgPaint 配置结束

        // 创建关键点画笔：黄色填充圆点
        val landmarkPaint = android.graphics.Paint().apply { // 创建 Android 原生 Paint 对象
            color = android.graphics.Color.YELLOW // 设置颜色为黄色
            style = android.graphics.Paint.Style.FILL // 设置填充样式
        } // landmarkPaint 配置结束

        // 获取原生 Android Canvas，用于调用 drawRect/drawCircle/drawText
        val canvas = drawContext.canvas.nativeCanvas // 将 Compose Canvas 转为 Android Canvas

        // 遍历每个检测到的人脸，绘制边界框、关键点和姓名标签
        detections.forEachIndexed { index, detection -> // 遍历检测结果，index 为索引，detection 为人脸数据
            // 计算缩放后的边界框坐标（从分析坐标映射到显示坐标）
            val left = detection.rect.left * scaleX // 左边界 x 坐标
            val top = detection.rect.top * scaleY // 上边界 y 坐标
            val right = detection.rect.right * scaleX // 右边界 x 坐标
            val bottom = detection.rect.bottom * scaleY // 下边界 y 坐标

            // 绘制绿色边界框矩形
            canvas.drawRect(left, top, right, bottom, boxPaint) // 绘制描边矩形

            // 绘制黄色关键点（左眼、右眼、鼻尖、左嘴角、右嘴角）
            detection.landmarks.forEach { (lx, ly) -> // 遍历 5 个关键点
                canvas.drawCircle( // 绘制填充圆点
                    lx * scaleX, // 缩放后的 x 坐标
                    ly * scaleY, // 缩放后的 y 坐标
                    (size.width / 400f).coerceAtLeast(3f), // 圆点半径，自适应屏幕大小，最小 3px
                    landmarkPaint // 使用黄色填充画笔
                ) // drawCircle 结束
            } // 关键点遍历结束

            // 获取当前人脸的识别名称，如果索引越界则使用默认值 "Face"
            val name = names.getOrElse(index) { "Face" } // 安全获取人名，越界返回 "Face"
            // 格式化置信度百分比（如 "95%"）
            val confText = "%.0f%%".format(detection.confidence * 100) // 将置信度转为百分比字符串
            // 拼接标签文字："人名 (置信度%)"
            val label = "$name ($confText)" // 组合显示文字

            // 测量标签文字的宽度（用于计算背景矩形大小）
            val textWidth = textPaint.measureText(label) // 测量文字像素宽度
            // 获取文字高度（等于 textSize）
            val textHeight = textPaint.textSize // 文字高度等于字号
            // 定义文字内边距
            val padding = 6f // 文字与背景边缘的间距

            // 计算标签背景的顶部位置：优先放在边界框上方，空间不足时放在下方
            val labelTop = if (top - textHeight - padding * 2 >= 0) { // 如果上方空间足够
                top - textHeight - padding * 2 // 标签放在边界框上方
            } else { // 上方空间不足
                bottom // 标签放在边界框下方
            } // labelTop 计算结束

            // 绘制半透明黑色背景矩形
            canvas.drawRect( // 绘制填充矩形作为文字背景
                left, // 左边界
                labelTop, // 上边界
                left + textWidth + padding * 2, // 右边界（文字宽度 + 左右内边距）
                labelTop + textHeight + padding * 2, // 下边界（文字高度 + 上下内边距）
                bgPaint // 使用半透明黑色填充画笔
            ) // 背景矩形绘制结束

            // 绘制绿色标签文字
            canvas.drawText( // 绘制文字
                label, // 文字内容："人名 (置信度%)"
                left + padding, // 文字 x 坐标（左边界 + 内边距）
                labelTop + textHeight + padding / 2, // 文字 y 坐标（基线位置）
                textPaint // 使用绿色粗体文字画笔
            ) // 文字绘制结束
        } // 人脸遍历结束
    } // Canvas 结束
} // FaceOverlay 函数结束

// 标记 CameraStatusBar 为可组合函数，显示底部检测状态栏
@Composable // 声明为 Compose 可组合函数
private fun CameraStatusBar( // 定义摄像头状态栏函数
    faceCount: Int, // 传入当前检测到的人脸数量
    modifier: Modifier = Modifier // 接收外部修饰符，默认为空 Modifier
) { // 函数体开始
    // 使用 Surface 组件作为状态栏容器，支持圆角和半透明背景
    Surface( // Material 3 Surface 组件
        modifier = modifier, // 应用外部修饰符
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), // 使用主题表面颜色，85% 不透明度
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) // 顶部左右圆角 16dp
    ) { // Surface 内容开始
        // 使用 Row 水平排列状态信息
        Row( // 水平布局容器
            modifier = Modifier // 开始链式修饰符
                .fillMaxWidth() // 填满宽度
                .padding(horizontal = 16.dp, vertical = 12.dp), // 水平 16dp、垂直 12dp 内边距
            horizontalArrangement = Arrangement.SpaceBetween, // 两端对齐排列
            verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
        ) { // Row 内容开始
            // 左侧：显示检测到的人脸数量
            Text( // 文本组件
                if (faceCount > 0) "检测到 $faceCount 张人脸" else "未检测到人脸", // 根据人脸数量显示不同文字
                style = MaterialTheme.typography.bodyMedium, // 使用中正文排版样式
                color = if (faceCount > 0) MaterialTheme.colorScheme.primary // 有人脸时使用主题主色
                else MaterialTheme.colorScheme.onSurfaceVariant // 无人脸时使用表面变体文字颜色
            ) // 左侧文本结束
            // 右侧：显示实时识别状态
            Text( // 文本组件
                "实时识别中", // 状态提示文字
                style = MaterialTheme.typography.bodySmall, // 使用小正文排版样式
                color = MaterialTheme.colorScheme.onSurfaceVariant // 使用表面变体文字颜色
            ) // 右侧文本结束
        } // Row 结束
    } // Surface 结束
} // CameraStatusBar 函数结束

/**
 * ImageProxy 转 Bitmap 扩展函数
 * 将 CameraX 的 YUV_420_888 格式图像转换为可被 ONNX 模型处理的 Bitmap
 * 转换流程: YUV_420_888 → NV21 → JPEG → Bitmap
 * 正确处理 pixelStride: 当 stride=2 时，V/U buffer 包含交错的 VU 数据
 */
private fun ImageProxy.toBitmap(): Bitmap? { // 扩展函数，ImageProxy 可直接调用
    try { // 尝试执行格式转换
        // 获取 YUV 三个平面的数据
        val yPlane = planes[0] // Y 亮度平面（全分辨率）
        val uPlane = planes[1] // U 色度平面（1/4 分辨率，4:2:0 采样）
        val vPlane = planes[2] // V 色度平面（1/4 分辨率，4:2:0 采样）

        // 获取各平面的 ByteBuffer
        val yBuffer = yPlane.buffer // Y 平面数据缓冲区
        val uBuffer = uPlane.buffer // U 平面数据缓冲区
        val vBuffer = vPlane.buffer // V 平面数据缓冲区

        // 获取 Y 平面数据大小和像素步长
        val ySize = yBuffer.remaining() // Y 平面字节数
        val pixelStride = vPlane.pixelStride // V 平面像素步长（1=分离平面，2=交错格式）

        // 计算 NV21 格式数据总大小：Y 平面 + 交错的 VU 数据
        val nv21Size = ySize + width * height / 2 // NV21 = Y + VU（色度为 1/4 分辨率，VU 交错）
        // 创建 NV21 数据数组
        val nv21 = ByteArray(nv21Size) // 分配 NV21 数据缓冲区

        // 复制 Y 平面数据（逐行处理，因为 rowStride 可能大于 width）
        val yRowStride = yPlane.rowStride // Y 平面行步长（可能包含填充字节）
        for (row in 0 until height) { // 遍历每一行
            yBuffer.position(row * yRowStride) // 定位到当前行起始位置
            yBuffer.get(nv21, row * width, width) // 复制当前行的有效数据（跳过填充）
        } // Y 平面复制结束

        // 复制 VU 交错数据（根据 pixelStride 选择不同策略）
        val vuRowStride = vPlane.rowStride // V 平面行步长
        val vuOffset = ySize // VU 数据在 NV21 数组中的起始偏移量
        if (pixelStride == 2) { // pixelStride=2: V buffer 已经是 [V0,U0,V1,U1,...] 交错格式
            // 直接从 V buffer 复制交错的 VU 数据
            for (row in 0 until height / 2) { // 遍历色度行（高度为亮度的 1/2）
                vBuffer.position(row * vuRowStride) // 定位到当前行起始位置
                vBuffer.get(nv21, vuOffset + row * width, width) // 复制交错的 VU 数据
            } // 交错复制结束
        } else { // pixelStride=1: V 和 U 是分离的平面，需要手动交错
            // 手动将分离的 V 和 U 数据交错排列为 NV21 格式
            for (row in 0 until height / 2) { // 遍历色度行
                val destRow = vuOffset + row * width // 计算目标数组的行起始位置
                vBuffer.position(row * vuRowStride) // 定位 V buffer 当前行
                uBuffer.position(row * uPlane.rowStride) // 定位 U buffer 当前行
                for (col in 0 until width / 2) { // 遍历色度列（宽度为亮度的 1/2）
                    nv21[destRow + col * 2] = vBuffer.get() // 偶数位置写入 V 值
                    nv21[destRow + col * 2 + 1] = uBuffer.get() // 奇数位置写入 U 值
                } // 色度列遍历结束
            } // 色度行遍历结束
        } // pixelStride 判断结束

        // 将 NV21 数据压缩为 JPEG
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null) // 创建 YuvImage 对象
        val out = ByteArrayOutputStream() // 创建字节输出流
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 85, out) // 压缩为 JPEG（质量 85%）
        val bytes = out.toByteArray() // 获取 JPEG 字节数组

        // 将 JPEG 字节数组解码为 Bitmap
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null // 解码失败返回 null

        // 处理摄像头图像旋转（大多数手机摄像头输出需要旋转 90° 或 270°）
        val rotation = imageInfo.rotationDegrees // 获取图像旋转角度
        return if (rotation != 0) { // 如果需要旋转
            val matrix = Matrix() // 创建变换矩阵
            matrix.postRotate(rotation.toFloat()) // 设置旋转角度
            // 应用旋转变换创建新的 Bitmap
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true) // 旋转 Bitmap
            bitmap.recycle() // 释放原始 Bitmap 内存
            rotated // 返回旋转后的 Bitmap
        } else { // 不需要旋转
            bitmap // 直接返回原始 Bitmap
        } // 旋转处理结束
    } catch (e: Exception) { // 捕获转换过程中的异常
        // 记录转换失败的错误日志
        Log.e(TAG, "ImageProxy→Bitmap 失败: ${e.message}", e) // 输出错误日志到 Logcat
        return null // 返回 null 表示转换失败
    } // try-catch 结束
} // toBitmap 扩展函数结束
