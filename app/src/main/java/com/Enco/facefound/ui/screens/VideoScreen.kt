package com.Enco.facefound.ui.screens // 声明当前文件所属的包路径，用于视频识别界面

// 导入Android Uri类，用于表示视频文件的统一资源标识符
import android.net.Uri
// 导入Jetpack Compose的ActivityResult启动器，用于启动外部活动并获取结果
import androidx.activity.compose.rememberLauncherForActivityResult
// 导入ActivityResult合约类，提供预定义的活动结果协议（如获取文件内容）
import androidx.activity.result.contract.ActivityResultContracts
// 导入Compose动画的可见性控制组件
import androidx.compose.animation.AnimatedVisibility
// 导入淡入动画效果
import androidx.compose.animation.fadeIn
// 导入淡出动画效果
import androidx.compose.animation.fadeOut
// 导入背景修饰符，用于设置组件的背景颜色
import androidx.compose.foundation.background
// 导入布局排列方式，用于控制子组件的排列方向和间距
import androidx.compose.foundation.layout.Arrangement
// 导入Box布局容器，用于堆叠显示子组件
import androidx.compose.foundation.layout.Box
// 导入Column布局容器，用于垂直排列子组件
import androidx.compose.foundation.layout.Column
// 导入Row布局容器，用于水平排列子组件
import androidx.compose.foundation.layout.Row
// 导入Spacer组件，用于在布局中创建空白间距
import androidx.compose.foundation.layout.Spacer
// 导入fillMaxSize修饰符，使组件填满父容器的所有可用空间
import androidx.compose.foundation.layout.fillMaxSize
// 导入fillMaxWidth修饰符，使组件填满父容器的宽度
import androidx.compose.foundation.layout.fillMaxWidth
// 导入height修饰符，用于设置组件的高度或创建垂直间距
import androidx.compose.foundation.layout.height
// 导入padding修饰符，用于在组件内部添加内边距
import androidx.compose.foundation.layout.padding
// 导入size修饰符，用于设置组件的固定尺寸
import androidx.compose.foundation.layout.size
// 导入width修饰符，用于设置组件的宽度或创建水平间距
import androidx.compose.foundation.layout.width
// 导入LazyColumn组件，用于高效显示可滚动的垂直列表
import androidx.compose.foundation.lazy.LazyColumn
// 导入items扩展函数，用于在LazyColumn中渲染数据列表项
import androidx.compose.foundation.lazy.items
// 导入圆角矩形形状，用于创建圆角卡片和按钮
import androidx.compose.foundation.shape.RoundedCornerShape
// 导入Material图标集
import androidx.compose.material.icons.Icons
// 导入完成状态图标
import androidx.compose.material.icons.filled.CheckCircle
// 导入播放箭头图标
import androidx.compose.material.icons.filled.PlayArrow
// 导入停止图标
import androidx.compose.material.icons.filled.Stop
// 导入视频文件图标
import androidx.compose.material.icons.filled.VideoFile
// 导入视频摄像机图标
import androidx.compose.material.icons.filled.Videocam
// 导入保存图标
import androidx.compose.material.icons.filled.Save
// 导入AlertDialog对话框组件
import androidx.compose.material3.AlertDialog
// 导入Button按钮组件
import androidx.compose.material3.Button
// 导入ButtonDefaults，用于自定义按钮的颜色等默认值
import androidx.compose.material3.ButtonDefaults
// 导入Card卡片组件
import androidx.compose.material3.Card
// 导入CardDefaults，用于自定义卡片的默认样式
import androidx.compose.material3.CardDefaults
// 导入圆形进度指示器组件
import androidx.compose.material3.CircularProgressIndicator
// 导入Icon图标组件
import androidx.compose.material3.Icon
// 导入线性进度条组件
import androidx.compose.material3.LinearProgressIndicator
// 导入MaterialTheme主题对象，用于访问当前主题的颜色和字体等
import androidx.compose.material3.MaterialTheme
// 导入带轮廓的按钮组件
import androidx.compose.material3.OutlinedButton
// 导入滑块组件，用于连续值的选择
import androidx.compose.material3.Slider
// 导入Text文本组件
import androidx.compose.material3.Text
// 导入TextButton文字按钮组件
import androidx.compose.material3.TextButton
// 导入@Composable注解，标记可组合函数
import androidx.compose.runtime.Composable
// 导入LaunchedEffect，在组合函数中执行挂起函数
import androidx.compose.runtime.LaunchedEffect
// 导入collectAsState，将Flow转换为Compose可观察的状态
import androidx.compose.runtime.collectAsState
// 导入getValue委托，用于从状态中读取值
import androidx.compose.runtime.getValue
// 导入mutableStateOf，创建可变的状态对象
import androidx.compose.runtime.mutableStateOf
// 导入remember，在重组期间记住状态
import androidx.compose.runtime.remember
// 导入setValue委托，用于更新状态的值
import androidx.compose.runtime.setValue
// 导入Alignment对齐方式，用于控制组件在容器中的位置
import androidx.compose.ui.Alignment
// 导入Modifier修饰符类，用于装饰和配置组件
import androidx.compose.ui.Modifier
// 导入clip修饰符，用于将组件裁剪为指定形状
import androidx.compose.ui.draw.clip
// 导入StrokeCap，用于设置进度条端点的样式
import androidx.compose.ui.graphics.StrokeCap
// 导入ContentScale，用于控制图片的缩放方式
import androidx.compose.ui.layout.ContentScale
// 导入FontWeight字体粗细设置
import androidx.compose.ui.text.font.FontWeight
// 导入TextOverflow，用于控制文本溢出时的处理方式
import androidx.compose.ui.text.style.TextOverflow
// 导入dp密度无关像素单位
import androidx.compose.ui.unit.dp
// 导入Coil的AsyncImage组件，用于异步加载和显示图片
import coil.compose.AsyncImage
// 导入人脸识别视图模型，包含视频处理相关的业务逻辑
import com.Enco.facefound.ui.viewmodel.FaceRecognitionViewModel

// 定义视频识别主屏幕的可组合函数
@Composable
// 函数签名：接收视图模型和可选的修饰符参数
fun VideoScreen(
    // 传入人脸识别视图模型实例，用于管理视频处理状态和操作
    viewModel: FaceRecognitionViewModel,
    // 可选的外部修饰符，允许父组件自定义此屏幕的外观
    modifier: Modifier = Modifier
) {
    // 从视图模型中收集UI状态流，转换为可观察的状态对象
    val uiState by viewModel.uiState.collectAsState()
    // 创建可变状态，控制是否显示视频信息详情
    var showVideoInfo by remember { mutableStateOf(false) }

    // 创建视频文件选择器的活动结果启动器
    val videoPicker = rememberLauncherForActivityResult(
        // 使用GetContent合约，允许用户选择设备上的文件
        ActivityResultContracts.GetContent()
    ) { uri ->
        // 如果用户成功选择了文件（uri不为空），则将视频URI设置到视图模型中
        uri?.let { viewModel.setVideoUri(it) }
    }

    // 启动副作用监听器，当视频处理状态发生变化时触发
    LaunchedEffect(uiState.videoProcessingState) {
        // 检查视频处理是否已完成或出错
        if (uiState.videoProcessingState is FaceRecognitionViewModel.VideoProcessingState.Completed ||
            uiState.videoProcessingState is FaceRecognitionViewModel.VideoProcessingState.Error) {
            // 处理完成或出错时显示视频信息
            showVideoInfo = true
        // 否则检查是否处于空闲状态且已有视频URI
        } else if (uiState.videoProcessingState is FaceRecognitionViewModel.VideoProcessingState.Idle &&
            uiState.videoUri != null) {
            // 空闲状态且有视频时也显示视频信息
            showVideoInfo = true
        }
    }

    // 使用LazyColumn创建可滚动的垂直列表布局
    LazyColumn(
        // 应用外部传入的修饰符，并添加16dp的内边距
        modifier = modifier.padding(16.dp),
        // 设置子项之间的垂直间距为16dp
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 第一个列表项：屏幕标题
        item {
            // 显示"视频识别"标题文本
            Text(
                // 标题文字内容
                "视频识别",
                // 使用Material主题中的小型标题排版样式
                style = MaterialTheme.typography.headlineSmall,
                // 设置字体粗细为粗体
                fontWeight = FontWeight.Bold
            )
        }

        // 第二个列表项：视频源选择卡片
        item {
            // 调用视频源卡片组件
            VideoSourceCard(
                // 传入当前选中的视频URI
                videoUri = uiState.videoUri,
                // 传入视频的详细信息（分辨率、时长等）
                videoInfo = uiState.videoInfo,
                // 传入选择视频的回调函数，启动视频文件选择器
                onSelectVideo = { videoPicker.launch("video/*") }
            )
        }

        // 第三个列表项：视频设置卡片
        item {
            // 调用视频设置卡片组件
            VideoSettingsCard(
                // 传入当前的相似度阈值
                threshold = uiState.videoThreshold,
                // 传入阈值变化的回调函数
                onThresholdChange = { viewModel.updateVideoThreshold(it) },
                // 传入当前的检测阈值
                detectionThreshold = uiState.videoDetectionThreshold,
                // 传入检测阈值变化的回调函数
                onDetectionThresholdChange = { viewModel.updateVideoDetectionThreshold(it) },
                // 传入当前的采样率
                sampleRate = uiState.videoSampleRate,
                // 传入采样率变化的回调函数
                onSampleRateChange = { viewModel.updateVideoSampleRate(it) }
            )
        }

        // 第四个列表项：视频处理状态显示
        item {
            // 调用视频处理状态组件
            VideoProcessingStatus(
                // 传入当前的处理状态
                processingState = uiState.videoProcessingState,
                // 传入处理进度值
                progress = uiState.videoProgress,
                // 传入已处理的帧数
                processedCount = uiState.videoProcessedCount
            )
        }

        // 第五个列表项：视频控制按钮
        item {
            // 调用视频控制按钮组件
            VideoControlButtons(
                // 传入当前的处理状态
                processingState = uiState.videoProcessingState,
                // 传入是否已选择视频的标志
                hasVideo = uiState.videoUri != null,
                // 传入开始处理的回调函数
                onStartProcessing = { viewModel.startVideoProcessing() },
                // 传入取消处理的回调函数
                onCancelProcessing = { viewModel.cancelVideoProcessing() },
                // 传入保存视频的回调函数
                onSaveVideo = { viewModel.saveVideoResult() }
            )
        }

        // 检查是否有已处理的视频帧结果
        if (uiState.videoProcessedFrames.isNotEmpty()) {
            // 列表项：处理结果预览标题
            item {
                // 显示处理结果预览标题，包含帧数信息
                Text(
                    // 显示帧数量的标题文本
                    "处理结果预览 (${uiState.videoProcessedFrames.size} 帧)",
                    // 使用中型标题排版样式
                    style = MaterialTheme.typography.titleMedium,
                    // 顶部添加8dp内边距
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 渲染已处理帧的预览列表，最多显示最近20帧
            items(
                // 取最后20个处理结果进行显示
                uiState.videoProcessedFrames.takeLast(20),
                // 使用帧索引作为列表项的唯一标识
                key = { it.frameIndex }
            ) { frameResult ->
                // 为每一帧调用预览卡片组件
                VideoFramePreviewCard(frameResult)
            }
        }

        // 底部间隔项，为底部导航栏留出空间
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// 定义视频源选择卡片的可组合函数
@Composable
// 函数签名：接收视频URI、视频信息和选择视频的回调
fun VideoSourceCard(
    // 当前选中的视频URI，可为空
    videoUri: Uri?,
    // 视频的详细信息，包含分辨率和时长等
    videoInfo: com.Enco.facefound.video.VideoProcessor.VideoInfo?,
    // 用户点击选择视频时的回调函数
    onSelectVideo: () -> Unit
) {
    // 创建卡片容器
    Card(
        // 设置卡片为16dp圆角矩形形状
        shape = RoundedCornerShape(16.dp),
        // 自定义卡片颜色
        colors = CardDefaults.cardColors(
            // 使用主题的表面变体颜色作为卡片背景
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        // 卡片填满父容器宽度
        modifier = Modifier.fillMaxWidth()
    ) {
        // 使用Column垂直排列卡片内容，添加16dp内边距
        Column(Modifier.padding(16.dp)) {
            // 显示"视频源"标题文本
            Text(
                // 标题文字
                "视频源",
                // 使用小型标题排版样式
                style = MaterialTheme.typography.titleSmall,
                // 设置半粗体字重
                fontWeight = FontWeight.SemiBold,
                // 使用主题的主色作为文字颜色
                color = MaterialTheme.colorScheme.primary
            )

            // 标题下方添加12dp间距
            Spacer(Modifier.height(12.dp))

            // 判断是否尚未选择视频
            if (videoUri == null) {
                // 未选择视频时显示占位提示区域
                Box(
                    modifier = Modifier
                        // 填满宽度
                        .fillMaxWidth()
                        // 设置高度为140dp
                        .height(140.dp)
                        // 裁剪为12dp圆角矩形
                        .clip(RoundedCornerShape(12.dp))
                        // 使用主题表面颜色作为背景
                        .background(MaterialTheme.colorScheme.surface),
                    // 内容居中对齐
                    contentAlignment = Alignment.Center
                ) {
                    // 垂直排列占位内容并水平居中
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 显示视频占位图标
                        Icon(
                            // 使用视频摄像机图标
                            Icons.Default.Videocam,
                            // 无障碍描述为空
                            contentDescription = null,
                            // 图标大小为48dp
                            modifier = Modifier.size(48.dp),
                            // 使用表面变体上的文字颜色
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // 图标与文字之间添加8dp间距
                        Spacer(Modifier.height(8.dp))
                        // 显示提示文字
                        Text(
                            // 提示用户选择视频文件
                            "选择视频文件开始识别",
                            // 使用表面变体上的文字颜色
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            // 使用正文小号排版样式
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                // 占位区域与按钮之间添加12dp间距
                Spacer(Modifier.height(12.dp))
                // 显示选择视频的按钮
                Button(
                    // 点击时触发选择视频的回调
                    onClick = onSelectVideo,
                    // 按钮填满宽度
                    modifier = Modifier.fillMaxWidth(),
                    // 按钮为10dp圆角
                    shape = RoundedCornerShape(10.dp)
                ) {
                    // 显示视频文件图标
                    Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    // 图标与文字之间添加8dp间距
                    Spacer(Modifier.width(8.dp))
                    // 按钮文字
                    Text("选择视频")
                }
            } else {
                // 已选择视频时显示视频信息行
                Row(
                    Modifier
                        // 填满宽度
                        .fillMaxWidth()
                        // 裁剪为10dp圆角矩形
                        .clip(RoundedCornerShape(10.dp))
                        // 使用主题表面颜色作为背景
                        .background(MaterialTheme.colorScheme.surface)
                        // 添加12dp内边距
                        .padding(12.dp),
                    // 垂直居中对齐
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 视频图标背景容器
                    Box(
                        Modifier
                            // 固定尺寸44dp
                            .size(44.dp)
                            // 裁剪为10dp圆角矩形
                            .clip(RoundedCornerShape(10.dp))
                            // 使用主色容器颜色作为背景
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        // 内容居中对齐
                        contentAlignment = Alignment.Center
                    ) {
                        // 显示视频文件图标
                        Icon(
                            // 使用视频文件图标
                            Icons.Default.VideoFile,
                            // 无障碍描述为空
                            contentDescription = null,
                            // 使用主题主色作为图标颜色
                            tint = MaterialTheme.colorScheme.primary,
                            // 图标大小为22dp
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    // 图标与文字之间添加12dp间距
                    Spacer(Modifier.width(12.dp))
                    // 使用Column显示视频文件名和详细信息，占据剩余空间
                    Column(Modifier.weight(1f)) {
                        // 显示视频文件名
                        Text(
                            // 获取URI的最后路径段作为文件名，如果为空则显示默认文字
                            videoUri.lastPathSegment ?: "视频文件",
                            // 设置中等字重
                            fontWeight = FontWeight.Medium,
                            // 最多显示1行
                            maxLines = 1,
                            // 溢出时显示省略号
                            overflow = TextOverflow.Ellipsis
                        )
                        // 检查视频信息是否存在且时长大于0
                        if (videoInfo != null && videoInfo.durationMs > 0) {
                            // 将毫秒转换为秒
                            val seconds = videoInfo.durationMs / 1000
                            // 显示视频分辨率和时长信息
                            Text(
                                // 格式化显示分辨率和时长（分:秒）
                                "${videoInfo.width}×${videoInfo.height} · ${seconds / 60}:${String.format("%02d", seconds % 60)}",
                                // 使用标签小号排版样式
                                style = MaterialTheme.typography.labelSmall,
                                // 使用表面变体上的文字颜色
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // 信息行与按钮之间添加8dp间距
                Spacer(Modifier.height(8.dp))
                // 显示重新选择视频的轮廓按钮
                OutlinedButton(
                    // 点击时触发选择视频的回调
                    onClick = onSelectVideo,
                    // 按钮填满宽度
                    modifier = Modifier.fillMaxWidth(),
                    // 按钮为10dp圆角
                    shape = RoundedCornerShape(10.dp)
                ) {
                    // 按钮文字
                    Text("重新选择")
                }
            }
        }
    }
}

// 定义视频识别设置卡片的可组合函数
@Composable
// 函数签名：接收阈值、阈值变化回调、采样率和采样率变化回调
fun VideoSettingsCard(
    // 当前的相似度阈值，范围0.0到1.0
    threshold: Float,
    // 阈值变化时的回调函数
    onThresholdChange: (Float) -> Unit,
    // 当前的检测置信度阈值
    detectionThreshold: Float,
    // 检测阈值变化时的回调函数
    onDetectionThresholdChange: (Float) -> Unit,
    // 当前的采样间隔帧数
    sampleRate: Int,
    // 采样率变化时的回调函数
    onSampleRateChange: (Int) -> Unit
) {
    // 创建设置卡片容器
    Card(
        // 设置16dp圆角矩形形状
        shape = RoundedCornerShape(16.dp),
        // 自定义卡片颜色
        colors = CardDefaults.cardColors(
            // 使用表面变体颜色作为背景
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        // 卡片填满宽度
        modifier = Modifier.fillMaxWidth()
    ) {
        // 使用Column垂直排列设置内容，添加16dp内边距
        Column(Modifier.padding(16.dp)) {
            // 显示"识别设置"标题
            Text(
                // 标题文字
                "识别设置",
                // 使用小型标题排版样式
                style = MaterialTheme.typography.titleSmall,
                // 设置半粗体字重
                fontWeight = FontWeight.SemiBold,
                // 使用主题主色
                color = MaterialTheme.colorScheme.primary
            )

            // 标题下方添加8dp间距
            Spacer(Modifier.height(8.dp))

            // 阈值设置的标签行
            Row(
                // 填满宽度
                Modifier.fillMaxWidth(),
                // 子项两端对齐
                horizontalArrangement = Arrangement.SpaceBetween,
                // 垂直居中对齐
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 显示"相似度阈值"标签
                Text("相似度阈值", style = MaterialTheme.typography.bodyMedium)
                // 显示当前阈值数值，保留两位小数
                Text(
                    // 格式化阈值为两位小数的字符串
                    String.format("%.2f", threshold),
                    // 使用标签中号排版样式
                    style = MaterialTheme.typography.labelMedium,
                    // 使用主题主色
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // 阈值滑块控件
            Slider(
                // 当前滑块的值
                value = threshold,
                // 值变化时的回调
                onValueChange = onThresholdChange,
                // 滑块的值范围从0.0到1.0
                valueRange = 0.0f..1.0f,
                // 滑块的离散步数为99步（加上两端共101个刻度）
                steps = 99,
                // 垂直方向添加4dp内边距
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // 阈值滑块下方添加4dp间距
            Spacer(Modifier.height(4.dp))

            // 检测阈值设置的标签行
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("检测阈值", style = MaterialTheme.typography.bodyMedium)
                Text(
                    String.format("%.2f", detectionThreshold),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // 检测阈值滑块控件
            Slider(
                value = detectionThreshold,
                onValueChange = onDetectionThresholdChange,
                valueRange = 0.0f..1.0f,
                steps = 99,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // 检测阈值滑块下方添加4dp间距
            Spacer(Modifier.height(4.dp))

            // 采样间隔设置的标签行
            Row(
                // 填满宽度
                Modifier.fillMaxWidth(),
                // 子项两端对齐
                horizontalArrangement = Arrangement.SpaceBetween,
                // 垂直居中对齐
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 显示"采样间隔"标签
                Text("采样间隔", style = MaterialTheme.typography.bodyMedium)
                // 显示当前采样间隔的描述
                Text(
                    // 格式化显示每N帧采样一次
                    "每 $sampleRate 帧",
                    // 使用标签中号排版样式
                    style = MaterialTheme.typography.labelMedium,
                    // 使用主题主色
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // 采样率滑块控件
            Slider(
                // 将整数采样率转换为浮点数作为滑块当前值
                value = sampleRate.toFloat(),
                // 值变化时转换为整数并回调
                onValueChange = { onSampleRateChange(it.toInt()) },
                // 滑块值范围从1到5
                valueRange = 1f..5f,
                // 滑块的离散步数为3步（加上两端共5个刻度）
                steps = 3,
                // 垂直方向添加4dp内边距
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

// 定义视频处理状态显示组件的可组合函数
@Composable
// 函数签名：接收处理状态、进度值和已处理帧数
fun VideoProcessingStatus(
    // 当前的视频处理状态
    processingState: FaceRecognitionViewModel.VideoProcessingState,
    // 处理进度，范围0.0到1.0
    progress: Float,
    // 已处理完成的帧数
    processedCount: Int
) {
    // 使用动画可见性组件，在非空闲状态时显示
    AnimatedVisibility(
        // 当处理状态不是空闲时可见
        visible = processingState !is FaceRecognitionViewModel.VideoProcessingState.Idle,
        // 进入时使用淡入动画
        enter = fadeIn(),
        // 退出时使用淡出动画
        exit = fadeOut()
    ) {
        // 创建状态信息卡片
        Card(
            // 根据处理状态设置不同的卡片背景颜色
            colors = CardDefaults.cardColors(
                // 根据状态选择背景色
                containerColor = when (processingState) {
                    // 处理完成状态使用主色容器
                    is FaceRecognitionViewModel.VideoProcessingState.Completed ->
                        MaterialTheme.colorScheme.primaryContainer
                    // 出错状态使用错误容器颜色
                    is FaceRecognitionViewModel.VideoProcessingState.Error ->
                        MaterialTheme.colorScheme.errorContainer
                    // 其他状态使用表面变体颜色
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            // 卡片填满宽度
            modifier = Modifier.fillMaxWidth()
        ) {
            // 使用Column垂直排列状态内容，添加16dp内边距
            Column(Modifier.padding(16.dp)) {
                // 状态信息的水平排列行
                Row(
                    // 垂直居中对齐
                    verticalAlignment = Alignment.CenterVertically,
                    // 子项之间水平间距12dp
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 根据不同的处理状态显示不同的内容
                    when (processingState) {
                        // 正在处理状态
                        is FaceRecognitionViewModel.VideoProcessingState.Processing -> {
                            // 显示圆形进度指示器
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            // 显示处理进度文字
                            Text(
                                // 显示已处理帧数
                                "正在处理... ${processedCount} 帧已完成",
                                // 使用正文中号排版样式
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        // 处理完成状态
                        is FaceRecognitionViewModel.VideoProcessingState.Completed -> {
                            // 显示完成图标
                            Icon(
                                // 使用完成勾选图标
                                Icons.Default.CheckCircle,
                                // 无障碍描述为空
                                contentDescription = null,
                                // 使用主题主色
                                tint = MaterialTheme.colorScheme.primary
                            )
                            // 显示完成信息文字
                            Text(
                                // 显示总处理帧数
                                "处理完成! 共 ${processedCount} 帧",
                                // 使用正文中号排版样式
                                style = MaterialTheme.typography.bodyMedium,
                                // 使用主题主色作为文字颜色
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        // 处理出错状态
                        is FaceRecognitionViewModel.VideoProcessingState.Error -> {
                            // 显示错误信息文字
                            Text(
                                // 显示错误消息
                                "处理出错: ${processingState.message}",
                                // 使用正文中号排版样式
                                style = MaterialTheme.typography.bodyMedium,
                                // 使用主题错误色作为文字颜色
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        // 视频编码状态
                        is FaceRecognitionViewModel.VideoProcessingState.Encoding -> {
                            // 显示圆形进度指示器
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            // 显示编码进度信息
                            Text(
                                // 优先显示自定义消息，否则显示默认编码文字
                                processingState.message ?: "正在编码视频...",
                                // 使用正文中号排版样式
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        // 其他状态（包括Idle）不显示任何内容
                        else -> {}
                    }
                }

                // 检查是否处于需要显示进度条的状态
                if (processingState is FaceRecognitionViewModel.VideoProcessingState.Processing ||
                    processingState is FaceRecognitionViewModel.VideoProcessingState.Encoding) {
                    // 进度条上方添加8dp间距
                    Spacer(Modifier.height(8.dp))
                    // 显示线性进度条
                    LinearProgressIndicator(
                        // 设置进度值
                        progress = { progress },
                        // 进度条填满宽度
                        modifier = Modifier.fillMaxWidth(),
                        // 进度条端点设置为圆头样式
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

// 定义视频控制按钮组件的可组合函数
@Composable
// 函数签名：接收处理状态、是否有视频标志和各种操作回调
fun VideoControlButtons(
    // 当前的视频处理状态
    processingState: FaceRecognitionViewModel.VideoProcessingState,
    // 是否已选择视频文件
    hasVideo: Boolean,
    // 开始处理视频的回调函数
    onStartProcessing: () -> Unit,
    // 取消处理的回调函数
    onCancelProcessing: () -> Unit,
    // 保存视频结果的回调函数
    onSaveVideo: () -> Unit
) {
    // 判断当前是否处于空闲状态
    val isIdle = processingState is FaceRecognitionViewModel.VideoProcessingState.Idle
    // 判断当前是否正在处理中
    val isProcessing = processingState is FaceRecognitionViewModel.VideoProcessingState.Processing
    // 判断当前是否处理已完成
    val isCompleted = processingState is FaceRecognitionViewModel.VideoProcessingState.Completed

    // 使用Row水平排列控制按钮
    Row(
        // 填满宽度
        Modifier.fillMaxWidth(),
        // 按钮之间水平间距8dp
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 空闲状态下显示开始识别按钮
        if (isIdle) {
            // 创建开始识别按钮
            Button(
                // 点击时触发开始处理回调
                onClick = onStartProcessing,
                // 仅在有视频时启用按钮
                enabled = hasVideo,
                // 按钮占据相等的权重
                modifier = Modifier.weight(1f),
                // 按钮为10dp圆角
                shape = RoundedCornerShape(10.dp)
            ) {
                // 显示播放箭头图标
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                // 图标与文字之间添加6dp间距
                Spacer(Modifier.width(6.dp))
                // 按钮文字
                Text("开始识别")
            }
        // 处理中状态下显示取消按钮
        } else if (isProcessing) {
            // 创建取消按钮
            Button(
                // 点击时触发取消处理回调
                onClick = onCancelProcessing,
                // 按钮占据相等的权重
                modifier = Modifier.weight(1f),
                // 按钮为10dp圆角
                shape = RoundedCornerShape(10.dp),
                // 自定义按钮颜色
                colors = ButtonDefaults.buttonColors(
                    // 使用主题错误色作为按钮背景
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                // 显示停止图标
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                // 图标与文字之间添加6dp间距
                Spacer(Modifier.width(6.dp))
                // 按钮文字
                Text("取消")
            }
        // 处理完成状态下显示重新识别和保存按钮
        } else if (isCompleted) {
            // 创建重新识别按钮
            Button(
                // 点击时触发开始处理回调（重新处理）
                onClick = onStartProcessing,
                // 按钮占据相等的权重
                modifier = Modifier.weight(1f),
                // 按钮为10dp圆角
                shape = RoundedCornerShape(10.dp)
            ) {
                // 显示播放箭头图标
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                // 图标与文字之间添加6dp间距
                Spacer(Modifier.width(6.dp))
                // 按钮文字
                Text("重新识别")
            }
            // 创建保存视频按钮
            Button(
                // 点击时触发保存视频回调
                onClick = onSaveVideo,
                // 按钮占据相等的权重
                modifier = Modifier.weight(1f),
                // 按钮为10dp圆角
                shape = RoundedCornerShape(10.dp)
            ) {
                // 显示保存图标
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                // 图标与文字之间添加6dp间距
                Spacer(Modifier.width(6.dp))
                // 按钮文字
                Text("保存视频")
            }
        }
    }
}

// 定义视频帧预览卡片组件的可组合函数
@Composable
// 函数签名：接收单帧处理结果数据
fun VideoFramePreviewCard(
    // 视频帧处理结果对象，包含帧索引、检测结果和识别名称
    frame: com.Enco.facefound.ui.viewmodel.FaceRecognitionViewModel.VideoFrameResult
) {
    // 创建帧预览卡片
    Card(
        modifier = Modifier
            // 卡片填满宽度
            .fillMaxWidth()
            // 垂直方向添加4dp外边距
            .padding(vertical = 4.dp),
        // 自定义卡片颜色
        colors = CardDefaults.cardColors(
            // 使用表面变体颜色作为背景
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        // 使用Column垂直排列帧信息，添加12dp内边距
        Column(Modifier.padding(12.dp)) {
            // 帧标题信息的水平排列行
            Row(
                // 垂直居中对齐
                verticalAlignment = Alignment.CenterVertically,
                // 填满宽度
                modifier = Modifier.fillMaxWidth()
            ) {
                // 显示帧编号
                Text(
                    // 格式化显示帧索引号
                    "帧 #${frame.frameIndex}",
                    // 使用标签中号排版样式
                    style = MaterialTheme.typography.labelMedium,
                    // 设置粗体字重
                    fontWeight = FontWeight.Bold
                )
                // 在帧编号和人脸数之间添加弹性间距
                Spacer(Modifier.weight(1f))
                // 显示检测到的人脸数量
                Text(
                    // 格式化显示人脸数量
                    "${frame.detections.size} 张人脸",
                    // 使用标签小号排版样式
                    style = MaterialTheme.typography.labelSmall,
                    // 使用表面变体上的文字颜色
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 检查是否有识别出的人名
            if (frame.names.isNotEmpty()) {
                // 人脸数与识别结果之间添加4dp间距
                Spacer(Modifier.height(4.dp))
                // 显示识别出的人名列表
                Text(
                    // 将所有人名用逗号连接并显示
                    "识别: ${frame.names.joinToString(", ")}",
                    // 使用正文小号排版样式
                    style = MaterialTheme.typography.bodySmall,
                    // 最多显示2行
                    maxLines = 2,
                    // 溢出时显示省略号
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
