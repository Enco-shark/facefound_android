package com.Enco.facerecognition.ui.screens // 声明当前文件所属的包路径，用于组织项目结构

// 以下为导入语句，引入项目所需的各类依赖库

import androidx.compose.animation.animateColorAsState // 导入颜色动画状态函数，用于实现颜色平滑过渡动画
import androidx.compose.animation.core.EaseOutCubic // 导入缓动曲线，用于按钮动画
import androidx.compose.animation.core.LinearEasing // 导入线性缓动，用于渐变背景
import androidx.compose.animation.core.Spring // 导入弹簧动画规格，用于弹性效果
import androidx.compose.animation.core.animateDpAsState // 导入Dp值动画状态，用于卡片阴影动画
import androidx.compose.animation.core.animateFloatAsState // 导入浮点动画状态，用于按钮缩放
import androidx.compose.animation.core.spring // 导入弹簧动画规格构建器
import androidx.compose.animation.core.tween // 导入补间动画规格构建器
import androidx.compose.foundation.Image // 导入基础Image组件，用于显示Bitmap图片
import androidx.compose.foundation.background // 导入背景修饰符，用于为组件设置背景颜色
import androidx.compose.foundation.clickable // 导入可点击修饰符，用于为组件添加点击交互
import androidx.compose.foundation.combinedClickable // 导入组合点击修饰符，用于支持单击、长按、双击
import androidx.compose.foundation.gestures.detectTapGestures // 导入点击手势检测，用于双击检测
import androidx.compose.foundation.gestures.rememberTransformableState // 导入可变换状态记忆函数，用于捏合缩放
import androidx.compose.foundation.gestures.transformable // 导入可变换修饰符，用于应用变换手势
import androidx.compose.foundation.layout.Arrangement // 导入排列方式，用于控制子组件的间距和对齐
import androidx.compose.foundation.layout.Box // 导入Box布局组件，用于层叠放置子组件
import androidx.compose.foundation.layout.Column // 导入Column布局组件，用于垂直排列子组件
import androidx.compose.foundation.layout.Row // 导入Row布局组件，用于水平排列子组件
import androidx.compose.foundation.layout.Spacer // 导入Spacer组件，用于在布局中创建空白间距
import androidx.compose.foundation.layout.fillMaxHeight // 导入fillMaxHeight修饰符，使组件填满父容器的高度
import androidx.compose.foundation.layout.fillMaxSize // 导入fillMaxSize修饰符，使组件填满父容器的宽高
import androidx.compose.foundation.layout.fillMaxWidth // 导入fillMaxWidth修饰符，使组件填满父容器的宽度
import androidx.compose.foundation.layout.height // 导入height修饰符，用于设置组件的固定高度
import androidx.compose.foundation.layout.padding // 导入padding修饰符，用于为组件添加内边距
import androidx.compose.foundation.layout.size // 导入size修饰符，用于同时设置组件的宽高
import androidx.compose.foundation.layout.width // 导入width修饰符，用于设置组件的固定宽度
import androidx.compose.foundation.lazy.LazyColumn // 导入LazyColumn组件，用于高效显示垂直滚动列表
import androidx.compose.foundation.lazy.items // 导入items函数，用于在LazyColumn中遍历数据列表
import androidx.compose.foundation.shape.CircleShape // 导入圆形形状，用于裁剪组件为圆形
import androidx.compose.foundation.shape.RoundedCornerShape // 导入圆角矩形形状，用于设置圆角效果
import androidx.compose.material.icons.Icons // 导入Material图标集合对象
import androidx.compose.material.icons.filled.CheckCircle // 导入勾选圆圈图标，用于表示成功状态
import androidx.compose.material.icons.filled.Clear // 导入清除图标，用于清空操作
import androidx.compose.material.icons.filled.Delete // 导入删除图标，用于删除操作
import androidx.compose.material.icons.filled.Edit // 导入编辑图标，用于编辑操作
import androidx.compose.material.icons.filled.Error // 导入错误图标，用于错误提示
import androidx.compose.material.icons.filled.Folder // 导入文件夹图标，用于文件管理相关功能
import androidx.compose.material.icons.filled.History // 导入历史记录图标，用于历史功能入口
import androidx.compose.material.icons.filled.Image // 导入图片图标，用于图片相关功能
import androidx.compose.material.icons.filled.Info // 导入信息图标，用于信息提示
import androidx.compose.material.icons.filled.Menu // 导入菜单图标，用于打开侧边抽屉
import androidx.compose.material.icons.filled.Palette // 导入调色板图标，用于主题切换功能
import androidx.compose.material.icons.filled.PlayArrow // 导入播放箭头图标，用于开始识别操作
import androidx.compose.material.icons.filled.Stop // 导入停止图标，用于取消操作
import androidx.compose.material.icons.filled.Refresh // 导入刷新图标，用于重新选择操作
import androidx.compose.material.icons.filled.Save // 导入保存图标，用于保存结果操作
import androidx.compose.material.icons.filled.Settings // 导入设置图标，用于设置页面入口
import androidx.compose.material.icons.filled.Videocam // 导入摄像机图标，用于视频识别功能
import androidx.compose.material3.AlertDialog // 导入警告对话框组件，用于弹出确认提示
import androidx.compose.material3.Button // 导入按钮组件，用于主要操作按钮
import androidx.compose.material3.ButtonDefaults // 导入按钮默认配置，用于自定义按钮颜色
import androidx.compose.material3.Card // 导入卡片组件，用于容器化展示内容
import androidx.compose.material3.CardDefaults // 导入卡片默认配置，用于设置卡片样式
import androidx.compose.material3.CenterAlignedTopAppBar // 导入居中对齐的顶部应用栏
import androidx.compose.material3.CircularProgressIndicator // 导入环形进度指示器，用于加载状态展示
import androidx.compose.material3.DrawerState // 导入抽屉状态类，用于管理抽屉的打开/关闭状态
import androidx.compose.material3.DrawerValue // 导入抽屉枚举值，表示打开或关闭状态
import androidx.compose.material3.ExperimentalMaterial3Api // 导入实验性Material3 API注解
import androidx.compose.material3.HorizontalDivider // 导入水平分割线组件，用于分隔内容区域
import androidx.compose.material3.Icon // 导入图标组件，用于显示矢量图标
import androidx.compose.material3.IconButton // 导入图标按钮组件，用于可点击的图标
import androidx.compose.material3.MaterialTheme // 导入Material主题对象，用于获取主题配色和排版
import androidx.compose.material3.ModalNavigationDrawer // 导入模态导航抽屉组件，用于侧滑菜单
import androidx.compose.material3.OutlinedButton // 导入描边按钮组件，用于次要操作按钮
import androidx.compose.material3.OutlinedTextField // 导入描边文本输入框，用于用户输入
import androidx.compose.material3.Scaffold // 导入脚手架组件，提供基本页面结构布局
import androidx.compose.material3.Slider // 导入滑块组件，用于数值调节
import androidx.compose.material3.Switch // 导入开关组件，用于布尔值切换
import androidx.compose.material3.Text // 导入文本组件，用于显示文字内容
import androidx.compose.material3.TextButton // 导入文本按钮组件，用于低优先级操作
import androidx.compose.material3.rememberDrawerState // 导入抽屉状态记忆函数，用于在重组间保持状态
import androidx.compose.runtime.Composable // 导入Composable注解，标记可组合函数
import androidx.compose.runtime.collectAsState // 导入collectAsState函数，用于将StateFlow转为Compose状态
import androidx.compose.runtime.getValue // 导入getValue委托，用于属性委托读取状态
import androidx.compose.runtime.mutableStateOf // 导入可变状态创建函数，用于创建本地状态
import androidx.compose.runtime.remember // 导入remember函数，用于在重组间缓存值
import androidx.compose.runtime.rememberCoroutineScope // 导入协程作用域记忆函数，用于在Composable中启动协程
import androidx.compose.runtime.setValue // 导入setValue委托，用于属性委托写入状态
import androidx.compose.ui.Alignment // 导入对齐方式，用于控制组件在容器中的对齐位置
import androidx.compose.ui.Modifier // 导入Modifier修饰符，用于修饰和配置组件
import androidx.compose.ui.draw.clip // 导入clip修饰符，用于裁剪组件形状
import androidx.compose.ui.draw.shadow // 导入shadow修饰符，用于添加阴影效果
import androidx.compose.ui.graphics.Color // 导入Color类，用于表示颜色值
import androidx.compose.ui.graphics.asImageBitmap // 导入asImageBitmap扩展函数，用于将Android Bitmap转为Compose ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer // 导入图形图层修饰符，用于应用缩放、旋转等变换
import com.Enco.facerecognition.ui.theme.GradientEnd // 导入渐变终点颜色，用于按钮渐变效果
import androidx.compose.ui.graphics.vector.ImageVector // 导入ImageVector类，用于表示矢量图标
import androidx.compose.ui.text.font.FontWeight // 导入FontWeight类，用于设置字体粗细
import androidx.compose.ui.text.style.TextAlign // 导入TextAlign类，用于设置文本对齐方式
import androidx.compose.ui.text.style.TextOverflow // 导入TextOverflow类，用于控制文本溢出行为
import androidx.compose.ui.unit.dp // 导入dp扩展属性，用于密度无关像素的尺寸单位
import androidx.lifecycle.viewmodel.compose.viewModel // 导入viewModel函数，用于获取ViewModel实例
import android.net.Uri // 导入Uri类，用于表示资源标识符（如图片路径）
import androidx.activity.compose.rememberLauncherForActivityResult // 导入ActivityResult启动器记忆函数
import androidx.activity.result.contract.ActivityResultContracts // 导入ActivityResult合约，定义启动器类型
import coil.compose.AsyncImage // 导入Coil的异步图片加载组件
import com.Enco.facerecognition.BuildConfig // 导入构建配置类，用于获取版本号等信息
import com.Enco.facerecognition.ui.viewmodel.FaceRecognitionViewModel // 导入人脸识别视图模型
import kotlinx.coroutines.launch // 导入协程启动函数
import java.text.SimpleDateFormat // 导入日期格式化类，用于格式化时间戳
import java.util.Date // 导入Date类，用于表示日期时间对象
import java.util.Locale // 导入Locale类，用于地区设置

// 以下是空行，用于分隔导入区域和代码区域

// --- 主屏幕 --- // 注释标记：以下是主屏幕相关的代码

@OptIn(ExperimentalMaterial3Api::class) // 标注此函数使用了实验性的Material3 API，需要显式选择启用
@Composable // 标记MainScreen为可组合函数，是Compose UI的基本构建单元
fun MainScreen( // 定义主屏幕函数，是应用的顶层UI入口
    viewModel: FaceRecognitionViewModel = viewModel() // 通过viewModel()获取或创建FaceRecognitionViewModel实例，默认注入
) { // 主函数体开始
    val uiState by viewModel.uiState.collectAsState() // 将ViewModel的StateFlow转为Compose状态，并通过委托自动收集更新
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed) // 创建并记忆抽屉的初始状态为关闭
    val scope = rememberCoroutineScope() // 创建并记忆协程作用域，用于在事件回调中启动协程

    // 图片选择器 // 注释标记：以下是图片选择器的配置
    val imagePicker = rememberLauncherForActivityResult( // 创建并记忆图片选择器的ActivityResult启动器
        ActivityResultContracts.GetContent() // 使用GetContent合约，允许用户从设备中选择文件
    ) { uri -> // 选择完成后的回调，接收选择结果的Uri
        uri?.let { viewModel.setInputImage(it) } // 如果Uri不为空，则调用ViewModel设置输入图片
    } // 图片选择器回调结束

    // 批量图片选择器 // 注释标记：以下是批量图片选择器的配置
    val batchImagePicker = rememberLauncherForActivityResult( // 创建并记忆批量图片选择器的ActivityResult启动器
        ActivityResultContracts.GetMultipleContents() // 使用GetMultipleContents合约，允许用户选择多个文件
    ) { uris -> // 选择完成后的回调，接收选择结果的Uri列表
        if (uris.isNotEmpty()) { // 如果选择了至少一张图片
            viewModel.startBatchRecognition(uris) // 调用ViewModel开始批量识别
        } // 结束非空检查
    } // 批量图片选择器回调结束

    // 模板选择器 // 注释标记：以下是模板选择器的配置
    val templatePicker = rememberLauncherForActivityResult( // 创建并记忆模板文件选择器的ActivityResult启动器
        ActivityResultContracts.GetContent() // 使用GetContent合约，允许用户选择任意类型文件
    ) { uri -> // 选择完成后的回调，接收选择结果的Uri
        uri?.let { viewModel.setTemplate(it) } // 如果Uri不为空，则调用ViewModel设置模板文件
    } // 模板选择器回调结束

    // 使用 remember 缓存回调，避免不必要的重组 // 注释说明缓存回调的目的是优化性能
    val onDrawerClose = remember<() -> Unit> { // 使用remember缓存关闭抽屉的回调函数引用
        { scope.launch { drawerState.close() } } // 在协程中执行关闭抽屉操作
    } // 回调缓存结束

    ModalNavigationDrawer( // 创建模态导航抽屉，提供从左侧滑出的菜单
        drawerState = drawerState, // 传入抽屉状态，控制抽屉的打开和关闭
        drawerContent = { // 定义抽屉内容区域的渲染逻辑
            DrawerContent( // 调用DrawerContent组件渲染侧边菜单内容
                viewModel = viewModel, // 传入ViewModel以便菜单项可以触发导航
                onItemClick = onDrawerClose // 传入菜单项点击后的关闭抽屉回调
            ) // DrawerContent组件调用结束
        } // 抽屉内容定义结束
    ) { // ModalNavigationDrawer的内容区域开始
        Scaffold( // 创建脚手架，提供顶部栏和内容区域的基础布局结构
            topBar = { // 定义顶部应用栏区域（优化B：渐变背景 + 滚动动态）
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "FaceRecognition",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "菜单",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "切换主题",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent, // 设置为透明，使用渐变背景
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                GradientEnd
                            )
                        )
                    )
                )
            } // 顶部栏定义结束
        ) { paddingValues -> // Scaffold的内容区域开始，接收系统提供的内边距值
            when (uiState.currentScreen) { // 根据当前屏幕状态进行条件分支渲染
                FaceRecognitionViewModel.Screen.Main -> { // 当前屏幕为主页时
                    MainContent( // 渲染主内容组件
                        viewModel = viewModel, // 传入ViewModel
                        imagePicker = { imagePicker.launch("image/*") }, // 传入图片选择器回调，启动图片MIME类型选择
                        batchImagePicker = { batchImagePicker.launch("image/*") }, // 传入批量图片选择器回调，启动多选图片MIME类型选择
                        templatePicker = { templatePicker.launch("*/*") }, // 传入模板选择器回调，启动任意文件类型选择
                        modifier = Modifier.padding(paddingValues) // 应用Scaffold提供的内边距
                    ) // MainContent调用结束
                } // 主页分支结束
                FaceRecognitionViewModel.Screen.Templates -> { // 当前屏幕为模板管理时
                    TemplatesScreen( // 渲染模板管理屏幕
                        viewModel = viewModel, // 传入ViewModel
                        modifier = Modifier.padding(paddingValues) // 应用Scaffold提供的内边距
                    ) // TemplatesScreen调用结束
                } // 模板管理分支结束
                FaceRecognitionViewModel.Screen.History -> { // 当前屏幕为识别历史时
                    HistoryScreen( // 渲染识别历史屏幕
                        viewModel = viewModel, // 传入ViewModel
                        modifier = Modifier.padding(paddingValues) // 应用Scaffold提供的内边距
                    ) // HistoryScreen调用结束
                } // 识别历史分支结束
                FaceRecognitionViewModel.Screen.Settings -> { // 当前屏幕为设置时
                    SettingsScreen( // 渲染设置屏幕
                        viewModel = viewModel, // 传入ViewModel
                        modifier = Modifier.padding(paddingValues) // 应用Scaffold提供的内边距
                    ) // SettingsScreen调用结束
                } // 设置分支结束
                FaceRecognitionViewModel.Screen.Video -> { // 当前屏幕为视频识别时
                    VideoScreen( // 渲染视频识别屏幕
                        viewModel = viewModel, // 传入ViewModel
                        modifier = Modifier.padding(paddingValues) // 应用Scaffold提供的内边距
                    ) // VideoScreen调用结束
                } // 视频识别分支结束
                FaceRecognitionViewModel.Screen.About -> { // 当前屏幕为关于时
                    AboutScreen( // 渲染关于屏幕
                        modifier = Modifier.padding(paddingValues) // 应用Scaffold提供的内边距
                    ) // AboutScreen调用结束
                } // 关于分支结束
            } // when条件分支结束
        } // Scaffold内容区域结束
    } // ModalNavigationDrawer内容区域结束
} // MainScreen函数结束

// 以下是空行，用于分隔主屏幕和侧滑菜单代码

// --- 侧滑菜单 --- // 注释标记：以下是侧滑菜单相关的代码

@Composable // 标记DrawerContent为可组合函数
fun DrawerContent( // 定义侧边抽屉内容函数
    viewModel: FaceRecognitionViewModel, // 接收ViewModel参数，用于导航操作
    onItemClick: () -> Unit // 接收菜单项点击回调，用于关闭抽屉
) { // 函数体开始
    val uiState by viewModel.uiState.collectAsState() // 收集ViewModel的UI状态，用于判断当前选中的屏幕
    val screens = listOf( // 定义屏幕列表，包含所有可导航的页面
        FaceRecognitionViewModel.Screen.Main to ("主页" to Icons.Default.Image), // 主页屏幕项，显示图片图标
        FaceRecognitionViewModel.Screen.Video to ("视频识别" to Icons.Default.Videocam), // 视频识别屏幕项，显示摄像机图标
        FaceRecognitionViewModel.Screen.Templates to ("模板管理" to Icons.Default.Folder), // 模板管理屏幕项，显示文件夹图标
        FaceRecognitionViewModel.Screen.History to ("识别历史" to Icons.Default.History), // 识别历史屏幕项，显示历史图标
        FaceRecognitionViewModel.Screen.Settings to ("设置" to Icons.Default.Settings), // 设置屏幕项，显示设置图标
        FaceRecognitionViewModel.Screen.About to ("关于" to Icons.Default.Info) // 关于屏幕项，显示信息图标
    ) // 屏幕列表定义结束

    Column( // 创建垂直布局容器，用于排列抽屉内的所有内容
        Modifier // 开始链式修饰符
            .fillMaxHeight() // 使列容器填满整个高度
            .background(MaterialTheme.colorScheme.surface) // 设置背景色为主题表面颜色
    ) { // Column内容区域开始
        // 抽屉头部：紧凑现代风
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 应用图标（紧凑，48dp）
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "FaceRecognition",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "离线人脸识别",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } // 抽屉头部结束

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) // 显示水平分割线，左右各留16dp边距

        Spacer(Modifier.height(8.dp)) // 创建8dp的垂直间距

        screens.forEachIndexed { _, (screen, pair) -> // 遍历屏幕列表，获取索引和解构出屏幕枚举及配对
            val (title, icon) = pair // 从配对中解构出标题文本和图标
            DrawerMenuItem( // 渲染单个抽屉菜单项
                title = title, // 传入菜单项标题
                icon = icon, // 传入菜单项图标
                screen = screen, // 传入对应的屏幕枚举值
                currentScreen = uiState.currentScreen, // 传入当前选中的屏幕用于高亮判断
                onClick = { // 定义菜单项点击事件
                    viewModel.navigateTo(screen) // 调用ViewModel导航到对应屏幕
                    onItemClick() // 执行点击回调（关闭抽屉）
                } // 点击事件定义结束
            ) // DrawerMenuItem调用结束
        } // forEachIndexed遍历结束

        Spacer(Modifier.weight(1f)) // 创建弹性空间，将下方内容推到底部

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) // 显示底部分割线，左右各留16dp边距

        Text( // 显示版本号文本
            "v${BuildConfig.VERSION_NAME}", // 使用字符串模板拼接版本号
            style = MaterialTheme.typography.labelSmall, // 使用小标签排版样式
            color = MaterialTheme.colorScheme.onSurfaceVariant, // 设置颜色为表面变体上的文字色
            modifier = Modifier.padding(16.dp) // 添加16dp的内边距
        ) // 版本号Text结束
    } // Column内容区域结束
} // DrawerContent函数结束

@Composable // 标记DrawerMenuItem为可组合函数
fun DrawerMenuItem( // 定义抽屉菜单项函数
    title: String, // 菜单项标题文本参数
    icon: ImageVector, // 菜单项图标参数
    screen: FaceRecognitionViewModel.Screen, // 此菜单项对应的屏幕枚举
    currentScreen: FaceRecognitionViewModel.Screen, // 当前选中的屏幕枚举
    onClick: () -> Unit // 点击回调函数参数
) { // 函数体开始
    val isSelected = screen == currentScreen // 判断当前菜单项是否被选中
    val bgColor by animateColorAsState( // 创建并记忆背景颜色的动画状态
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) // 选中时使用更明显的半透明主色容器色
        else Color.Transparent, // 未选时使用透明色
        label = "drawerBg" // 动画标签，用于Compose工具调试
    ) // 背景颜色动画结束
    val contentColor by animateColorAsState( // 创建并记忆内容颜色的动画状态
        if (isSelected) MaterialTheme.colorScheme.primary // 选中时使用主题主色
        else MaterialTheme.colorScheme.onSurfaceVariant, // 未选时使用表面变体上的文字色，更柔和
        label = "drawerContent" // 动画标签，用于Compose工具调试
    ) // 内容颜色动画结束

    Row( // 水平布局：紧凑现代风
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { // Row内容区域开始
        Box( // 图标容器
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    } // Row内容区域结束
} // DrawerMenuItem函数结束

// 以下是空行，用于分隔菜单和主页内容代码

// --- 主页内容 --- // 注释标记：以下是主页内容相关的代码

@Composable // 标记MainContent为可组合函数
fun MainContent( // 定义主页内容函数
    viewModel: FaceRecognitionViewModel, // 接收ViewModel参数
    imagePicker: () -> Unit, // 接收图片选择器启动回调
    batchImagePicker: () -> Unit, // 接收批量图片选择器启动回调
    templatePicker: () -> Unit, // 接收模板选择器启动回调
    modifier: Modifier = Modifier // 接收外部传入的修饰符，默认为空Modifier
) { // 函数体开始
    val uiState by viewModel.uiState.collectAsState() // 收集ViewModel的UI状态

    LazyColumn( // 创建可滚动的垂直懒加载列表（参照视频识别界面风格）
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp), // 左右20dp留白，上下16dp
        verticalArrangement = Arrangement.spacedBy(24.dp) // 子项之间设置24dp垂直间距
    ) { // LazyColumn内容区域开始
        // 页面标题 // 参照VideoScreen风格
        item { // 列表项：页面标题
            Text( // 显示"图片识别"标题
                "图片识别", // 标题文本
                style = MaterialTheme.typography.headlineSmall, // 使用小标题排版样式
                fontWeight = FontWeight.Bold // 设置粗体字重
            ) // 标题Text结束
        } // 标题项结束
        
        // 图片源卡片 // 参照VideoSourceCard风格
        item { // 列表项：图片源卡片
            ImageSourceCard( // 渲染图片源卡片
                imageUri = uiState.inputImageUri, // 传入当前图片Uri
                resultBitmap = uiState.resultBitmap, // 传入识别结果Bitmap
                onSelectImage = { imagePicker() }, // 传入选择图片回调
                onReselectImage = { imagePicker() } // 传入重新选择图片回调
            ) // ImageSourceCard调用结束
        } // 图片源项结束
        
        // 识别设置卡片 // 参照VideoSettingsCard风格，合并两个阈值设置
        item { // 列表项：识别设置卡片
            RecognitionSettingsCard( // 渲染识别设置卡片
                threshold = uiState.threshold, // 传入当前相似度阈值
                onThresholdChange = { viewModel.updateThreshold(it) }, // 传入阈值变更回调
                detectionThreshold = uiState.detectionThreshold, // 传入当前检测阈值
                onDetectionThresholdChange = { viewModel.updateDetectionThreshold(it) }, // 传入检测阈值变更回调
                templateName = uiState.templateName, // 传入当前模板名称
                onLoadTemplate = { templatePicker() } // 传入加载模板回调
            ) // RecognitionSettingsCard调用结束
        } // 识别设置项结束
        
        // 处理状态显示 // 参照VideoProcessingStatus风格
        item { // 列表项：处理状态
            ProcessingStatus( // 渲染处理状态组件
                isProcessing = uiState.isProcessing, // 传入是否正在处理
                statusMessage = uiState.statusMessage, // 传入状态消息
                isReady = uiState.isReady // 传入是否就绪
            ) // ProcessingStatus调用结束
        } // 处理状态项结束
        
        // 控制按钮 // 参照VideoControlButtons风格
        item { // 列表项：控制按钮
            ImageControlButtons( // 渲染图片控制按钮
                isProcessing = uiState.isProcessing, // 传入是否正在处理
                hasImage = uiState.inputImageUri != null, // 传入是否已选择图片
                hasResult = uiState.resultBitmap != null, // 传入是否有识别结果
                onStartRecognition = { viewModel.startRecognition() }, // 传入开始识别回调
                onSaveImage = { viewModel.saveResultImage() }, // 传入保存图片回调
                onBatchRecognize = { batchImagePicker() } // 传入批量识别回调
            ) // ImageControlButtons调用结束
        } // 控制按钮项结束
        
        // 识别结果预览 // 参照VideoFramePreviewCard风格
        if (uiState.resultBitmap != null) { // 如果有识别结果
            item { // 列表项：识别结果标题
                Text( // 显示结果预览标题
                    "识别结果", // 标题文本
                    style = MaterialTheme.typography.titleMedium, // 使用中标题排版样式
                    modifier = Modifier.padding(top = 8.dp) // 顶部添加8dp内边距
                ) // 标题Text结束
            } // 结果标题项结束
            
            item { // 列表项：识别结果卡片
                Card( // 创建结果卡片
                    modifier = Modifier.fillMaxWidth(), // 填满宽度
                    shape = RoundedCornerShape(16.dp), // 16dp圆角
                    colors = CardDefaults.cardColors( // 设置卡片颜色
                        containerColor = MaterialTheme.colorScheme.surfaceVariant // 使用表面变体色
                    )
                ) { // Card内容开始
                    Column(Modifier.padding(16.dp)) { // 创建垂直布局，添加16dp内边距
                        Text( // 显示识别完成提示
                            "✅ 识别完成", // 完成提示文本
                            style = MaterialTheme.typography.bodyMedium, // 使用正文中号排版样式
                            fontWeight = FontWeight.SemiBold, // 设置半粗体字重
                            color = MaterialTheme.colorScheme.primary // 使用主题主色
                        ) // 提示Text结束
                        
                        Spacer(Modifier.height(8.dp)) // 创建8dp垂直间距
                        
                        Text( // 显示处理结果提示
                            "识别结果已显示在上方预览区域\n点击「保存结果」可将结果保存到相册", // 提示文本（两行）
                            style = MaterialTheme.typography.bodySmall, // 使用正文小号排版样式
                            color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
                        ) // 提示Text结束
                    } // Column内容结束
                } // Card内容结束
            } // 结果卡片项结束
        } // 结果预览条件判断结束
        
        // 日志卡片 // 保留日志显示功能
        item { // 列表项：日志卡片
            LogCard(logs = uiState.logs) // 渲染日志卡片，传入日志列表
        } // 日志卡片项结束
    } // LazyColumn内容区域结束
} // MainContent函数结束

// 以下是空行，用于分隔主页内容和模板管理代码

// --- 模板管理屏幕 --- // 注释标记：以下是模板管理屏幕相关的代码

@Composable // 标记TemplatesScreen为可组合函数
fun TemplatesScreen( // 定义模板管理屏幕函数
    viewModel: FaceRecognitionViewModel, // 接收ViewModel参数
    modifier: Modifier = Modifier // 接收外部修饰符，默认为空Modifier
) { // 函数体开始
    val uiState by viewModel.uiState.collectAsState() // 收集ViewModel的UI状态
    var showRenameDialog by remember { mutableStateOf<String?>(null) } // 创建并记忆重命名对话框状态，存储待重命名的模板名，null表示不显示
    var showDeleteDialog by remember { mutableStateOf<String?>(null) } // 创建并记忆删除对话框状态，存储待删除的模板名，null表示不显示
    var showClearDialog by remember { mutableStateOf(false) } // 创建并记忆清空对话框状态，布尔值控制是否显示

    val templatePicker = rememberLauncherForActivityResult( // 创建并记忆模板文件选择器
        ActivityResultContracts.GetContent() // 使用GetContent合约，允许选择任意类型文件
    ) { uri -> // 选择完成后的回调
        uri?.let { viewModel.setTemplate(it) } // 如果Uri不为空，调用ViewModel设置模板
    } // 模板选择器回调结束

    LazyColumn(modifier.padding(16.dp)) { // 创建懒加载列表，应用修饰符并添加16dp内边距
        item { // 列表项：页面标题
            Text( // 显示页面标题
                "模板管理", // 标题文本
                style = MaterialTheme.typography.headlineSmall, // 使用小标题排版样式
                modifier = Modifier.padding(bottom = 16.dp) // 底部添加16dp内边距
            ) // 标题Text结束
        } // 标题项结束

        item { // 列表项：操作按钮行
            Row( // 创建水平布局排列按钮
                Modifier.fillMaxWidth(), // 填满宽度
                horizontalArrangement = Arrangement.spacedBy(8.dp) // 按钮之间设置8dp水平间距
            ) { // Row内容开始
                Button( // 导入NPZ按钮
                    onClick = { templatePicker.launch("*/*") }, // 点击时启动文件选择器
                    modifier = Modifier.weight(1f) // 占据等分的可用宽度
                ) { // Button内容开始
                    Icon(Icons.Default.Folder, contentDescription = null) // 显示文件夹图标
                    Spacer(Modifier.width(8.dp)) // 创建8dp水平间距
                    Text("导入 NPZ") // 按钮文字"导入 NPZ"
                } // 导入按钮结束
                if (uiState.templateList.isNotEmpty()) { // 如果模板列表不为空
                    OutlinedButton( // 清空全部按钮使用描边样式
                        onClick = { showClearDialog = true }, // 点击时显示清空确认对话框
                        modifier = Modifier.weight(1f) // 占据等分的可用宽度
                    ) { // OutlinedButton内容开始
                        Icon(Icons.Default.Delete, contentDescription = null) // 显示删除图标
                        Spacer(Modifier.width(8.dp)) // 创建8dp水平间距
                        Text("清空全部") // 按钮文字"清空全部"
                    } // 清空按钮结束
                } // 条件分支结束
            } // Row内容结束
        } // 操作按钮行项结束

        item { Spacer(Modifier.height(12.dp)) } // 列表项：创建12dp垂直间距

        if (uiState.templateList.isEmpty()) { // 如果模板列表为空
            item { // 列表项：优化后的空状态提示卡片（方案C）
                Card( // 创建卡片容器
                    colors = CardDefaults.cardColors( // 设置卡片颜色
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // 使用半透明的表面变体色
                    ), // 卡片颜色配置结束
                    modifier = Modifier.fillMaxWidth(), // 填满宽度
                    shape = RoundedCornerShape(20.dp) // 增大圆角至20dp
                ) { // Card内容开始
                    Column( // 创建垂直布局容器
                        modifier = Modifier.padding(32.dp), // 增大内边距至32dp，更宽敞
                        horizontalAlignment = Alignment.CenterHorizontally, // 水平居中对齐
                        verticalArrangement = Arrangement.Center // 垂直居中对齐
                    ) { // Column内容开始
                        // 图标容器
                        Box( // 创建图标容器Box
                            modifier = Modifier // 开始链式修饰符
                                .size(80.dp) // 设置大小为80dp
                                .clip(CircleShape) // 裁剪为圆形
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)), // 设置半透明主色容器背景
                            contentAlignment = Alignment.Center // 内容居中对齐
                        ) { // Box内容开始
                            Icon( // 显示文件夹图标
                                Icons.Default.Folder, // 使用文件夹图标
                                contentDescription = "暂无模板", // 无障碍描述
                                modifier = Modifier.size(40.dp), // 增大图标至40dp
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) // 设置颜色为半透明主色
                            ) // Icon结束
                        } // Box内容结束
                        
                        Spacer(Modifier.height(20.dp)) // 增大垂直间距至20dp
                        
                        Text( // 显示空状态标题
                            "暂无模板", // 标题文本
                            style = MaterialTheme.typography.titleMedium, // 使用中标题排版样式
                            fontWeight = FontWeight.SemiBold // 设置字体为半粗体
                        ) // 标题Text结束
                        
                        Spacer(Modifier.height(8.dp)) // 创建8dp垂直间距
                        
                        Text( // 显示空状态说明
                            "请导入 NPZ 模板文件\n导入后将自动持久化保存", // 说明文本（两行）
                            style = MaterialTheme.typography.bodyMedium, // 使用中等正文排版样式
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // 设置颜色为表面变体上的文字色
                            textAlign = TextAlign.Center // 居中对齐
                        ) // 说明Text结束
                        
                        Spacer(Modifier.height(24.dp)) // 增大垂直间距至24dp
                        
                        Button( // 创建引导按钮
                            onClick = { templatePicker.launch("*/*") }, // 点击时启动模板选择器
                            shape = RoundedCornerShape(12.dp), // 设置12dp圆角
                            colors = ButtonDefaults.buttonColors( // 设置按钮颜色
                                containerColor = MaterialTheme.colorScheme.primary, // 设置容器颜色为主题主色
                                contentColor = MaterialTheme.colorScheme.onPrimary // 设置内容颜色为主色上的对比色
                            )
                        ) { // Button内容开始
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) // 显示文件夹图标
                            Spacer(Modifier.width(8.dp)) // 创建8dp水平间距
                            Text("导入模板", style = MaterialTheme.typography.labelMedium) // 按钮文字"导入模板"
                        } // Button结束
                    } // Column内容结束
                } // Card内容结束
            } // 空状态项结束
        } else { // 模板列表不为空时
            item { // 列表项：模板统计信息卡片
                Card( // 创建卡片容器
                    colors = CardDefaults.cardColors( // 设置卡片颜色
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) // 使用半透明主色容器色
                    ), // 卡片颜色配置结束
                    modifier = Modifier // 开始链式修饰符
                        .fillMaxWidth() // 填满宽度
                        .padding(bottom = 12.dp) // 底部添加12dp内边距
                ) { // Card内容开始
                    Row( // 创建水平布局
                        Modifier.padding(12.dp), // 添加12dp内边距
                        verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
                    ) { // Row内容开始
                        Icon( // 显示文件夹图标
                            Icons.Default.Folder, // 使用文件夹图标
                            contentDescription = null, // 无障碍描述为空
                            tint = MaterialTheme.colorScheme.primary // 设置颜色为主题主色
                        ) // Icon结束
                        Spacer(Modifier.width(8.dp)) // 创建8dp水平间距
                        Text( // 显示模板统计文本
                            "${uiState.templateList.size} 个模板已持久化 · 下次启动无需重新导入", // 使用字符串模板显示模板数量
                            style = MaterialTheme.typography.bodySmall, // 使用小正文排版样式
                            color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
                        ) // 统计文本Text结束
                    } // Row内容结束
                } // Card内容结束
            } // 模板统计项结束

            items(uiState.templateList, key = { it.name }) { template -> // 遍历模板列表，以模板名为key进行高效渲染
                TemplateItemCard( // 渲染单个模板项卡片
                    template = template, // 传入模板数据对象
                    onDelete = { showDeleteDialog = it }, // 传入删除回调，触发显示删除对话框
                    onRename = { showRenameDialog = it } // 传入重命名回调，触发显示重命名对话框
                ) // TemplateItemCard调用结束
            } // 模板列表遍历结束
        } // else分支结束
    } // LazyColumn内容区域结束

    showRenameDialog?.let { name -> // 如果重命名对话框需要显示，获取待重命名的模板名
        var newName by remember { mutableStateOf(name) } // 创建并记忆新名称输入状态，初始值为当前模板名
        AlertDialog( // 创建警告对话框
            onDismissRequest = { showRenameDialog = null }, // 点击外部或返回键时关闭对话框
            title = { Text("重命名模板") }, // 对话框标题
            text = { // 对话框内容区域
                OutlinedTextField( // 创建描边文本输入框
                    value = newName, // 绑定当前输入的名称值
                    onValueChange = { newName = it }, // 输入变化时更新状态
                    label = { Text("新名称") } // 输入框标签文本
                ) // OutlinedTextField结束
            }, // 对话框内容结束
            confirmButton = { // 确认按钮区域
                Button(onClick = { // 创建确认按钮并定义点击事件
                    if (newName.isNotEmpty()) { // 如果新名称不为空
                        viewModel.renameTemplate(name, newName) // 调用ViewModel重命名模板
                        showRenameDialog = null // 关闭对话框
                    } // 条件判断结束
                }) { // Button内容开始
                    Text("确定") // 按钮文字"确定"
                } // 确认按钮结束
            }, // 确认按钮区域结束
            dismissButton = { // 取消按钮区域
                TextButton(onClick = { showRenameDialog = null }) { // 创建文本按钮，点击时关闭对话框
                    Text("取消") // 按钮文字"取消"
                } // 取消按钮结束
            } // 取消按钮区域结束
        ) // AlertDialog结束
    } // 重命名对话框let块结束

    showDeleteDialog?.let { name -> // 如果删除对话框需要显示，获取待删除的模板名
        AlertDialog( // 创建警告对话框
            onDismissRequest = { showDeleteDialog = null }, // 点击外部或返回键时关闭对话框
            title = { Text("删除模板") }, // 对话框标题
            text = { Text("确定要删除模板 \"$name\" 吗？") }, // 对话框内容，使用字符串模板插入模板名
            confirmButton = { // 确认按钮区域
                Button(onClick = { // 创建确认按钮并定义点击事件
                    viewModel.removeTemplate(name) // 调用ViewModel删除模板
                    showDeleteDialog = null // 关闭对话框
                }) { // Button内容开始
                    Text("删除") // 按钮文字"删除"
                } // 确认按钮结束
            }, // 确认按钮区域结束
            dismissButton = { // 取消按钮区域
                TextButton(onClick = { showDeleteDialog = null }) { // 创建文本按钮，点击时关闭对话框
                    Text("取消") // 按钮文字"取消"
                } // 取消按钮结束
            } // 取消按钮区域结束
        ) // AlertDialog结束
    } // 删除对话框let块结束

    if (showClearDialog) { // 如果需要显示清空全部对话框
        AlertDialog( // 创建警告对话框
            onDismissRequest = { showClearDialog = false }, // 点击外部或返回键时关闭对话框
            title = { Text("清空全部模板") }, // 对话框标题
            text = { Text("确定要清空所有 ${uiState.templateList.size} 个人脸模板吗？\n\n注意：模板数据将永久删除，需要重新导入 NPZ 文件。") }, // 对话框内容，显示模板数量和警告信息
            confirmButton = { // 确认按钮区域
                Button(onClick = { // 创建确认按钮并定义点击事件
                    uiState.templateList.forEach { viewModel.removeTemplate(it.name) } // 遍历所有模板并逐一删除
                    showClearDialog = false // 关闭对话框
                }) { // Button内容开始
                    Text("全部清空") // 按钮文字"全部清空"
                } // 确认按钮结束
            }, // 确认按钮区域结束
            dismissButton = { // 取消按钮区域
                TextButton(onClick = { showClearDialog = false }) { // 创建文本按钮，点击时关闭对话框
                    Text("取消") // 按钮文字"取消"
                } // 取消按钮结束
            } // 取消按钮区域结束
        ) // AlertDialog结束
    } // 清空对话框条件判断结束
} // TemplatesScreen函数结束

@Composable // 标记TemplateItemCard为可组合函数
fun TemplateItemCard( // 定义模板项卡片函数
    template: FaceRecognitionViewModel.TemplateItem, // 接收模板数据对象参数
    onDelete: (String) -> Unit, // 接收删除回调，参数为模板名称
    onRename: (String) -> Unit // 接收重命名回调，参数为模板名称
) { // 函数体开始
    Card( // 创建卡片容器
        modifier = Modifier // 开始链式修饰符
            .fillMaxWidth() // 填满宽度
            .padding(vertical = 4.dp) // 上下各添加4dp内边距
    ) { // Card内容开始
        Row( // 创建水平布局排列模板信息和操作按钮
            verticalAlignment = Alignment.CenterVertically, // 垂直居中对齐
            modifier = Modifier.padding(16.dp) // 添加16dp内边距
        ) { // Row内容开始
            Box( // 创建头像容器Box
                modifier = Modifier // 开始链式修饰符
                    .size(48.dp) // 设置大小为48dp
                    .clip(CircleShape) // 裁剪为圆形
                    .background(MaterialTheme.colorScheme.primaryContainer), // 设置背景色为主色容器色
                contentAlignment = Alignment.Center // 内容居中对齐
            ) { // Box内容开始
                Text( // 显示模板名称首字母
                    template.name.firstOrNull()?.toString() ?: "?", // 获取名称首字符，为空时显示问号
                    color = MaterialTheme.colorScheme.onPrimaryContainer // 设置颜色为主色容器上的文字色
                ) // Text结束
            } // Box头像容器结束
            Spacer(Modifier.width(16.dp)) // 创建16dp水平间距
            Text( // 显示模板名称
                template.name, // 文本内容为模板名称
                modifier = Modifier.weight(1f), // 占据剩余可用宽度
                maxLines = 1, // 限制最多显示1行
                overflow = TextOverflow.Ellipsis // 溢出时显示省略号
            ) // Text结束
            IconButton(onClick = { onRename(template.name) }) { // 创建重命名图标按钮
                Icon(Icons.Default.Edit, contentDescription = "重命名") // 显示编辑图标，无障碍描述为"重命名"
            } // 重命名按钮结束
            IconButton(onClick = { onDelete(template.name) }) { // 创建删除图标按钮
                Icon(Icons.Default.Delete, contentDescription = "删除") // 显示删除图标，无障碍描述为"删除"
            } // 删除按钮结束
        } // Row内容结束
    } // Card内容结束
} // TemplateItemCard函数结束

// 以下是空行，用于分隔模板管理和识别历史代码

// --- 识别历史屏幕 --- // 注释标记：以下是识别历史屏幕相关的代码

@Composable // 标记HistoryScreen为可组合函数
fun HistoryScreen( // 定义识别历史屏幕函数
    viewModel: FaceRecognitionViewModel, // 接收ViewModel参数
    modifier: Modifier = Modifier // 接收外部修饰符，默认为空Modifier
) { // 函数体开始
    val uiState by viewModel.uiState.collectAsState() // 收集ViewModel的UI状态
    var showClearDialog by remember { mutableStateOf(false) } // 创建并记忆清空对话框显示状态

    LazyColumn(modifier.padding(16.dp)) { // 创建懒加载列表，应用修饰符并添加16dp内边距
        item { // 列表项：页面标题行
            Row( // 创建水平布局排列标题和清空按钮
                modifier = Modifier.fillMaxWidth(), // 填满宽度
                verticalAlignment = Alignment.CenterVertically, // 垂直居中对齐
                horizontalArrangement = Arrangement.SpaceBetween // 两端对齐排列
            ) { // Row内容开始
                Text( // 显示页面标题
                    "识别历史", // 标题文本
                    style = MaterialTheme.typography.headlineSmall // 使用小标题排版样式
                ) // 标题Text结束
                if (uiState.recognitionHistory.isNotEmpty()) { // 如果识别历史列表不为空
                    Button(onClick = { showClearDialog = true }) { // 创建清空按钮，点击时显示确认对话框
                        Icon(Icons.Default.Clear, contentDescription = "清空") // 显示清除图标
                        Spacer(Modifier.width(4.dp)) // 创建4dp水平间距
                        Text("清空") // 按钮文字"清空"
                    } // 清空按钮结束
                } // 条件判断结束
            } // Row内容结束
        } // 标题行项结束

        if (uiState.recognitionHistory.isEmpty()) { // 如果识别历史列表为空
            item { // 列表项：空状态提示卡片
                Card( // 创建卡片容器
                    colors = CardDefaults.cardColors( // 设置卡片颜色
                        containerColor = MaterialTheme.colorScheme.surfaceVariant // 使用表面变体色作为背景
                    ), // 卡片颜色配置结束
                    modifier = Modifier.fillMaxWidth() // 填满宽度
                ) { // Card内容开始
                    Text( // 显示空状态提示
                        "暂无识别历史", // 提示文本
                        modifier = Modifier.padding(24.dp) // 添加24dp内边距
                    ) // Text结束
                } // Card内容结束
            } // 空状态项结束
        } else { // 识别历史列表不为空时
            items(uiState.recognitionHistory, key = { it.id }) { item -> // 遍历历史列表，以id为key进行高效渲染
                HistoryItemCard(item) // 渲染单个历史项卡片
            } // 历史列表遍历结束
        } // else分支结束
    } // LazyColumn内容区域结束

    if (showClearDialog) { // 如果需要显示清空历史对话框
        AlertDialog( // 创建警告对话框
            onDismissRequest = { showClearDialog = false }, // 点击外部或返回键时关闭对话框
            title = { Text("清空历史") }, // 对话框标题
            text = { Text("确定要清空所有识别历史吗？") }, // 对话框确认内容
            confirmButton = { // 确认按钮区域
                Button(onClick = { // 创建确认按钮并定义点击事件
                    viewModel.clearHistory() // 调用ViewModel清空历史记录
                    showClearDialog = false // 关闭对话框
                }) { // Button内容开始
                    Text("清空") // 按钮文字"清空"
                } // 确认按钮结束
            }, // 确认按钮区域结束
            dismissButton = { // 取消按钮区域
                TextButton(onClick = { showClearDialog = false }) { // 创建文本按钮，点击时关闭对话框
                    Text("取消") // 按钮文字"取消"
                } // 取消按钮结束
            } // 取消按钮区域结束
        ) // AlertDialog结束
    } // 清空对话框条件判断结束
} // HistoryScreen函数结束

@Composable // 标记HistoryItemCard为可组合函数
fun HistoryItemCard(item: FaceRecognitionViewModel.RecognitionHistoryItem) { // 定义历史项卡片函数，接收历史记录数据对象
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) } // 创建并记忆日期格式化器，格式为"年-月-日 时:分:秒"
    Card( // 创建卡片容器
        modifier = Modifier // 开始链式修饰符
            .fillMaxWidth() // 填满宽度
            .padding(vertical = 4.dp) // 上下各添加4dp内边距
    ) { // Card内容开始
        Column(Modifier.padding(16.dp)) { // 创建垂直布局，添加16dp内边距
            Text( // 显示识别时间
                dateFormat.format(item.timestamp), // 使用日期格式化器格式化时间戳
                style = MaterialTheme.typography.labelSmall, // 使用小标签排版样式
                color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
            ) // 时间Text结束
            Spacer(Modifier.height(4.dp)) // 创建4dp垂直间距
            Text( // 显示识别结果
                "识别结果: ${item.recognizedNames.joinToString(", ")}", // 将识别到的名字用逗号连接显示
                style = MaterialTheme.typography.bodyMedium // 使用中正文排版样式
            ) // 结果Text结束
            Spacer(Modifier.height(4.dp)) // 创建4dp垂直间距
            Text( // 显示处理耗时
                "耗时: ${item.processingTimeMs}ms", // 显示处理耗时毫秒数
                style = MaterialTheme.typography.labelSmall // 使用小标签排版样式
            ) // 耗时Text结束
        } // Column内容结束
    } // Card内容结束
} // HistoryItemCard函数结束

// 以下是空行，用于分隔识别历史和设置代码

// --- 设置屏幕 --- // 注释标记：以下是设置屏幕相关的代码

@Composable // 标记SettingsScreen为可组合函数
fun SettingsScreen( // 定义设置屏幕函数
    viewModel: FaceRecognitionViewModel, // 接收ViewModel参数
    modifier: Modifier = Modifier // 接收外部修饰符，默认为空Modifier
) { // 函数体开始
    val uiState by viewModel.uiState.collectAsState() // 收集ViewModel的UI状态

    LazyColumn(modifier.padding(16.dp)) { // 创建懒加载列表，应用修饰符并添加16dp内边距
        item { // 列表项：页面标题
            Text( // 显示页面标题
                "设置", // 标题文本
                style = MaterialTheme.typography.headlineSmall, // 使用小标题排版样式
                modifier = Modifier.padding(bottom = 16.dp) // 底部添加16dp内边距
            ) // 标题Text结束
        } // 标题项结束

        item { // 列表项：外观设置组
            SettingGroup(title = "外观") { // 渲染"外观"设置组
                Row( // 创建水平布局排列主题切换选项
                    modifier = Modifier // 开始链式修饰符
                        .fillMaxWidth() // 填满宽度
                        .padding(vertical = 12.dp), // 上下各添加12dp内边距
                    verticalAlignment = Alignment.CenterVertically, // 垂直居中对齐
                    horizontalArrangement = Arrangement.SpaceBetween // 两端对齐排列
                ) { // Row内容开始
                    Text("深色主题") // 显示"深色主题"标签
                    Switch( // 创建开关组件
                        checked = uiState.isDarkTheme, // 绑定深色主题状态
                        onCheckedChange = { viewModel.toggleTheme() } // 切换时调用ViewModel切换主题
                    ) // Switch结束
                } // Row内容结束
            } // 外观设置组结束
        } // 外观设置项结束

        item { // 列表项：识别设置组
            SettingGroup(title = "识别设置") { // 渲染"识别设置"设置组
                Column(Modifier.padding(vertical = 4.dp)) { // 创建垂直布局，上下各添加4dp内边距
                    Row( // 创建水平布局排列阈值标签和数值
                        Modifier.fillMaxWidth(), // 填满宽度
                        horizontalArrangement = Arrangement.SpaceBetween, // 两端对齐排列
                        verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
                    ) { // Row内容开始
                        Text("相似度阈值", style = MaterialTheme.typography.bodyMedium) // 显示"相似度阈值"标签
                        Text( // 显示当前阈值数值
                            String.format("%.2f", uiState.threshold), // 将阈值格式化为两位小数
                            style = MaterialTheme.typography.labelMedium, // 使用中标签排版样式
                            color = MaterialTheme.colorScheme.primary // 设置颜色为主题主色
                        ) // 阈值数值Text结束
                    } // Row内容结束
                    Slider( // 创建滑块组件
                        value = uiState.threshold, // 绑定当前阈值
                        onValueChange = { viewModel.updateThreshold(it) }, // 值变化时调用ViewModel更新阈值
                        valueRange = 0.0f..1.0f, // 设置值范围为0到1
                        steps = 99, // 设置步数为99（共100个刻度）
                        modifier = Modifier.padding(vertical = 4.dp) // 上下各添加4dp内边距
                    ) // Slider结束
                    Row( // 创建水平布局排列范围标签
                        Modifier.fillMaxWidth(), // 填满宽度
                        horizontalArrangement = Arrangement.SpaceBetween // 两端对齐排列
                    ) { // Row内容开始
                        Text("宽松", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // 左侧显示"宽松"标签
                        Text("严格", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // 右侧显示"严格"标签
                    } // Row内容结束
                } // Column内容结束

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // 显示水平分割线，上下各留8dp间距

                Row( // 创建水平布局排列自动降采样选项
                    modifier = Modifier // 开始链式修饰符
                        .fillMaxWidth() // 填满宽度
                        .padding(vertical = 4.dp), // 上下各添加4dp内边距
                    verticalAlignment = Alignment.CenterVertically, // 垂直居中对齐
                    horizontalArrangement = Arrangement.SpaceBetween // 两端对齐排列
                ) { // Row内容开始
                    Text("自动降采样大图") // 显示"自动降采样大图"标签
                    Switch( // 创建开关组件
                        checked = uiState.imageDownsample, // 绑定图片降采样状态
                        onCheckedChange = { viewModel.setImageDownsample(it) } // 切换时调用ViewModel设置降采样选项
                    ) // Switch结束
                } // Row内容结束
            } // 识别设置组结束
        } // 识别设置项结束
    } // LazyColumn内容区域结束
} // SettingsScreen函数结束

@Composable // 标记SettingGroup为可组合函数
fun SettingGroup(title: String, content: @Composable () -> Unit) { // 定义设置组函数，接收标题和可组合内容
    Card( // 创建卡片容器
        modifier = Modifier // 开始链式修饰符
            .fillMaxWidth() // 填满宽度
            .padding(vertical = 6.dp), // 上下各添加6dp内边距
        shape = RoundedCornerShape(14.dp), // 设置14dp圆角
        colors = CardDefaults.cardColors( // 设置卡片颜色
            containerColor = MaterialTheme.colorScheme.surfaceVariant // 使用表面变体色作为背景
        ) // 卡片颜色配置结束
    ) { // Card内容开始
        Column(Modifier.padding(16.dp)) { // 创建垂直布局，添加16dp内边距
            Text( // 显示设置组标题
                title, // 文本内容为标题
                style = MaterialTheme.typography.titleSmall, // 使用小标题排版样式
                fontWeight = FontWeight.SemiBold, // 设置字体为半粗体
                color = MaterialTheme.colorScheme.primary, // 设置颜色为主题主色
                modifier = Modifier.padding(bottom = 12.dp) // 底部添加12dp内边距
            ) // 标题Text结束
            content() // 调用内容lambda渲染设置组内的子组件
        } // Column内容结束
    } // Card内容结束
} // SettingGroup函数结束

// 以下是空行，用于分隔设置和复用组件代码

// --- 复用组件 --- // 注释标记：以下是可复用的UI组件

@Composable // 标记StatusCard为可组合函数
fun StatusCard(statusMessage: String, isReady: Boolean) { // 定义状态卡片函数，接收状态消息和就绪标志
    // 根据状态选择渐变颜色：就绪=绿渐变，未就绪=橙红渐变
    val gradientColors = if (isReady) // 条件判断：是否就绪
        listOf(Color(0xFF00E676).copy(alpha = 0.15f), Color(0xFF00E676).copy(alpha = 0.05f)) // 就绪：绿色渐变（淡绿→更淡绿）
    else // 未就绪
        listOf(Color(0xFFFF9100).copy(alpha = 0.15f), Color(0xFFFF9100).copy(alpha = 0.05f)) // 未就绪：橙色渐变（淡橙→更淡橙）

    val iconTint = if (isReady) Color(0xFF00E676) else Color(0xFFFF9100) // 根据状态选择图标颜色：就绪=绿，未就绪=橙
    val statusText = if (isReady) "✅ 就绪" else "⚠️ 未就绪" // 根据状态选择状态前缀文本

    Card( // 创建卡片容器
        modifier = Modifier.fillMaxWidth(), // 填满宽度
        shape = RoundedCornerShape(18.dp), // 增大圆角至18dp，更现代
        colors = CardDefaults.cardColors(containerColor = Color.Transparent), // 容器透明，由内部Box绘制渐变
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // 添加轻微阴影，增加层次感
    ) { // Card内容开始
        Box( // 用Box实现渐变背景
            Modifier
                .fillMaxWidth()
                .background(brush = androidx.compose.ui.graphics.Brush.horizontalGradient(gradientColors)) // 水平渐变背景
                .padding(horizontal = 16.dp, vertical = 14.dp) // 内边距
        ) {
            Row( // 创建水平布局排列图标和文本
                Modifier.fillMaxWidth(), // 填满宽度
                verticalAlignment = Alignment.CenterVertically, // 垂直居中对齐
                horizontalArrangement = Arrangement.spacedBy(12.dp) // 子组件之间12dp水平间距
            ) { // Row内容开始
                // 状态图标容器（带背景圆角）
                Box( // 创建图标容器Box
                    Modifier // 开始链式修饰符
                        .size(38.dp) // 设置大小为38dp
                        .clip(RoundedCornerShape(12.dp)) // 裁剪为12dp圆角矩形
                        .background(iconTint.copy(alpha = 0.18f)), // 设置背景色为图标颜色（淡色）
                    contentAlignment = Alignment.Center // 内容居中对齐
                ) { // 图标Box内容开始
                    Icon( // 显示状态图标
                        if (isReady) Icons.Default.CheckCircle else Icons.Default.Info, // 就绪时显示勾选图标，否则显示信息图标
                        contentDescription = null, // 无障碍描述为空
                        tint = iconTint, // 图标颜色根据状态变化
                        modifier = Modifier.size(22.dp) // 设置图标大小为22dp
                    ) // Icon结束
                } // 图标Box内容结束

                Column(Modifier.weight(1f)) { // 创建垂直布局，占据剩余宽度
                    Text( // 显示状态标签
                        statusText, // 文本内容为状态前缀
                        style = MaterialTheme.typography.labelMedium, // 使用中等标签排版样式
                        fontWeight = FontWeight.SemiBold, // 设置字体为半粗体
                        color = iconTint // 颜色与图标一致
                    ) // 状态标签Text结束
                    Text( // 显示详细状态消息
                        statusMessage, // 文本内容为详细状态消息
                        style = MaterialTheme.typography.bodySmall, // 使用小正文排版样式
                        color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
                    ) // 详细消息Text结束
                } // Column结束
            } // Row内容结束
        } // Box结束
    } // Card内容结束
} // StatusCard函数结束

@Composable // 标记ImagePreviewCard为可组合函数
fun ImagePreviewCard( // 定义图片预览卡片函数
    imageUri: Uri?, // 接收图片Uri参数，可为null
    resultBitmap: android.graphics.Bitmap?, // 接收识别结果Bitmap参数，可为null
    onSelectImage: () -> Unit, // 接收选择图片回调
    onReselectImage: () -> Unit // 接收重新选择图片回调
) { // 函数体开始
    Card( // 创建卡片容器
        modifier = Modifier.fillMaxWidth(), // 填满宽度
        shape = RoundedCornerShape(20.dp), // 增大圆角至20dp，更现代
        colors = CardDefaults.cardColors( // 设置卡片颜色
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) // 使用半透明的表面变体色，更精致
        ), // 卡片颜色配置结束
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // 添加轻微阴影，增加层次感
    ) { // Card内容开始
        Box( // 创建Box容器用于层叠放置图片和按钮
            modifier = Modifier // 开始链式修饰符
                .fillMaxWidth() // 填满宽度
                .height(360.dp), // 增大高度至360dp，提供更宽敞的预览区域
            contentAlignment = Alignment.Center // 内容居中对齐
        ) { // Box内容开始
            when { // 根据不同条件显示不同内容
                resultBitmap != null -> { // 如果有识别结果Bitmap
                    Image( // 显示识别结果图片
                        bitmap = resultBitmap.asImageBitmap(), // 将Android Bitmap转为Compose ImageBitmap
                        contentDescription = "识别结果", // 无障碍描述为"识别结果"
                        modifier = Modifier // 开始链式修饰符
                            .fillMaxSize() // 填满整个容器
                            .clip(RoundedCornerShape(20.dp)), // 裁剪为20dp圆角，与卡片圆角一致
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop // 设置图片裁剪模式
                    ) // Image结束
                } // 识别结果分支结束
                imageUri != null -> { // 如果有选择的图片Uri
                    AsyncImage( // 使用Coil异步加载图片
                        model = imageUri, // 传入图片Uri作为加载源
                        contentDescription = "预览图片", // 无障碍描述为"预览图片"
                        modifier = Modifier // 开始链式修饰符
                            .fillMaxSize() // 填满整个容器
                            .clip(RoundedCornerShape(20.dp)), // 裁剪为20dp圆角
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop // 设置图片裁剪模式
                    ) // AsyncImage结束
                } // 图片预览分支结束
                else -> { // 如果没有任何图片
                    Column( // 创建垂直布局容器
                        modifier = Modifier // 开始链式修饰符
                            .fillMaxSize() // 填满整个容器
                            .clickable { onSelectImage() }, // 添加点击事件
                        horizontalAlignment = Alignment.CenterHorizontally, // 水平居中对齐
                        verticalArrangement = Arrangement.Center // 垂直居中对齐
                    ) { // Column内容开始
                        Box( // 创建图标容器，添加背景
                            modifier = Modifier // 开始链式修饰符
                                .size(80.dp) // 设置大小为80dp
                                .clip(RoundedCornerShape(20.dp)) // 裁剪为20dp圆角
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)), // 设置半透明主色容器背景
                            contentAlignment = Alignment.Center // 内容居中对齐
                        ) { // Box内容开始
                            Icon( // 显示选择图片图标
                                Icons.Default.Image, // 使用图片图标
                                contentDescription = "选择图片", // 无障碍描述为"选择图片"
                                modifier = Modifier.size(40.dp), // 增大图标大小为40dp
                                tint = MaterialTheme.colorScheme.primary // 设置颜色为主题主色
                            ) // Icon结束
                        } // Box内容结束
                        
                        Spacer(Modifier.height(16.dp)) // 创建16dp垂直间距
                        
                        Text( // 显示提示文本
                            "点击选择图片", // 提示文本
                            style = MaterialTheme.typography.bodyLarge, // 使用大正文排版样式
                            fontWeight = FontWeight.Medium, // 设置字体为中等粗细
                            color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
                        ) // Text结束
                        
                        Spacer(Modifier.height(4.dp)) // 创建4dp垂直间距
                        
                        Text( // 显示补充说明
                            "支持 JPG、PNG 格式", // 补充说明文本
                            style = MaterialTheme.typography.bodySmall, // 使用小正文排版样式
                            color = MaterialTheme.colorScheme.outline // 设置颜色为轮廓色
                        ) // Text结束
                    } // Column内容结束
                } // 空状态分支结束
            } // when条件分支结束
            
            if (imageUri != null || resultBitmap != null) { // 如果有图片或识别结果
                Button( // 创建重新选择按钮
                    onClick = onReselectImage, // 点击时触发重新选择回调
                    modifier = Modifier // 开始链式修饰符
                        .align(Alignment.BottomEnd) // 在Box右下角对齐
                        .padding(16.dp), // 增大内边距至16dp
                    shape = RoundedCornerShape(12.dp), // 增大圆角至12dp
                    colors = ButtonDefaults.buttonColors( // 设置按钮颜色
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), // 使用半透明的主色
                        contentColor = MaterialTheme.colorScheme.onPrimary // 设置内容颜色为主色上的对比色
                    ) // 按钮颜色配置结束
                ) { // Button内容开始
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) // 显示刷新图标，大小16dp
                    Spacer(Modifier.width(6.dp)) // 增大水平间距至6dp
                    Text("重新选择", style = MaterialTheme.typography.labelMedium) // 显示"重新选择"文本
                } // Button结束
            } // 条件判断结束
        } // Box内容结束
    } // Card内容结束
} // ImagePreviewCard函数结束

@Composable // 标记ControlPanel为可组合函数
fun ControlPanel( // 定义控制面板函数
    templateName: String?, // 接收模板名称参数，可为null
    onLoadTemplate: () -> Unit, // 接收加载模板回调
    onRecognize: () -> Unit, // 接收开始识别回调
    onBatchRecognize: () -> Unit, // 接收批量识别回调
    onSaveImage: () -> Unit, // 接收保存图片回调
    isProcessing: Boolean, // 接收是否正在处理的标志
    canSave: Boolean // 接收是否可以保存的标志
) { // 函数体开始
    Card( // 创建卡片容器
        shape = RoundedCornerShape(20.dp), // 增大圆角至20dp，更现代
        colors = CardDefaults.cardColors( // 设置卡片颜色
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) // 使用半透明的表面变体色，更精致
        ), // 卡片颜色配置结束
        modifier = Modifier.fillMaxWidth(), // 填满宽度
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // 添加轻微阴影，增加层次感
    ) { // Card内容开始
        Column( // 创建垂直布局
            modifier = Modifier.padding(20.dp), // 增大内边距至20dp，更宽敞
            verticalArrangement = Arrangement.spacedBy(16.dp) // 设置子组件之间16dp垂直间距
        ) { // Column内容开始
            OutlinedButton( // 创建描边按钮用于加载模板
                onClick = onLoadTemplate, // 点击时触发加载模板回调
                modifier = Modifier.fillMaxWidth(), // 填满宽度
                shape = RoundedCornerShape(14.dp), // 增大圆角至14dp
                colors = ButtonDefaults.outlinedButtonColors( // 设置按钮颜色
                    contentColor = MaterialTheme.colorScheme.primary // 设置内容颜色为主题主色
                )
            ) { // OutlinedButton内容开始
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp)) // 增大图标至20dp
                Spacer(Modifier.width(10.dp)) // 增大水平间距至10dp
                Text( // 显示模板名称或默认提示
                    templateName ?: "加载模板", // 如果有模板名则显示，否则显示"加载模板"
                    maxLines = 1, // 限制最多显示1行
                    overflow = TextOverflow.Ellipsis, // 溢出时显示省略号
                    style = MaterialTheme.typography.bodyMedium, // 使用中等正文排版样式
                    fontWeight = FontWeight.Medium // 设置字体为中等粗细
                ) // Text结束
            } // OutlinedButton结束
            
            Row( // 创建水平布局排列识别和保存按钮
                Modifier.fillMaxWidth(), // 填满宽度
                horizontalArrangement = Arrangement.spacedBy(12.dp) // 增大按钮间距至12dp
            ) { // Row内容开始
                Button( // 创建开始识别按钮
                    onClick = onRecognize, // 点击时触发识别回调
                    enabled = !isProcessing, // 未在处理时才可点击
                    modifier = Modifier.weight(1f), // 占据等分的可用宽度
                    shape = RoundedCornerShape(14.dp), // 增大圆角至14dp
                    colors = ButtonDefaults.buttonColors( // 设置按钮颜色
                        containerColor = MaterialTheme.colorScheme.primary, // 设置容器颜色为主题主色
                        contentColor = MaterialTheme.colorScheme.onPrimary // 设置内容颜色为主色上的对比色
                    )
                ) { // Button内容开始
                    if (isProcessing) { // 如果正在处理中
                        CircularProgressIndicator( // 显示环形进度指示器
                            modifier = Modifier.size(20.dp), // 增大指示器至20dp
                            strokeWidth = 2.5.dp, // 增大线条宽度至2.5dp
                            color = MaterialTheme.colorScheme.onPrimary // 设置颜色为主色上的对比色
                        ) // CircularProgressIndicator结束
                    } else { // 未在处理中
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp)) // 增大图标至20dp
                    } // 条件分支结束
                    Spacer(Modifier.width(8.dp)) // 增大水平间距至8dp
                    Text( // 根据处理状态显示不同文字
                        if (isProcessing) "识别中..." else "开始识别", // 文字内容
                        style = MaterialTheme.typography.bodyMedium, // 使用中等正文排版样式
                        fontWeight = FontWeight.SemiBold // 设置字体为半粗体
                    ) // Text结束
                } // 识别按钮结束
                
                Button( // 创建保存按钮
                    onClick = onSaveImage, // 点击时触发保存回调
                    enabled = canSave, // 有结果时才可点击
                    modifier = Modifier.weight(1f), // 占据等分的可用宽度
                    shape = RoundedCornerShape(14.dp), // 增大圆角至14dp
                    colors = ButtonDefaults.buttonColors( // 设置按钮颜色
                        containerColor = MaterialTheme.colorScheme.secondary, // 设置容器颜色为主题第二色
                        contentColor = MaterialTheme.colorScheme.onSecondary // 设置内容颜色为第二色上的对比色
                    )
                ) { // Button内容开始
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp)) // 增大图标至20dp
                    Spacer(Modifier.width(8.dp)) // 增大水平间距至8dp
                    Text( // 按钮文字"保存"
                        "保存", // 文字内容
                        style = MaterialTheme.typography.bodyMedium, // 使用中等正文排版样式
                        fontWeight = FontWeight.SemiBold // 设置字体为半粗体
                    ) // Text结束
                } // 保存按钮结束
            } // Row内容结束
            
            OutlinedButton( // 创建描边按钮用于批量识别
                onClick = onBatchRecognize, // 点击时触发批量识别回调
                enabled = !isProcessing, // 未在处理时才可点击
                modifier = Modifier.fillMaxWidth(), // 填满宽度
                shape = RoundedCornerShape(14.dp), // 增大圆角至14dp
                colors = ButtonDefaults.outlinedButtonColors( // 设置按钮颜色
                    contentColor = MaterialTheme.colorScheme.tertiary // 设置内容颜色为主题第三色
                )
            ) { // OutlinedButton内容开始
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp)) // 增大图标至20dp
                Spacer(Modifier.width(10.dp)) // 增大水平间距至10dp
                Text( // 按钮文字"批量识别"
                    "批量识别", // 文字内容
                    style = MaterialTheme.typography.bodyMedium, // 使用中等正文排版样式
                    fontWeight = FontWeight.Medium // 设置字体为中等粗细
                ) // Text结束
            } // 批量识别按钮结束
        } // Column内容结束
    } // Card内容结束
} // ControlPanel函数结束

@Composable // 标记ThresholdSlider为可组合函数
fun ThresholdSlider(threshold: Float, onThresholdChange: (Float) -> Unit, label: String = "相似度阈值") { // 定义阈值滑块函数，接收当前阈值、变更回调和标签
    Card( // 创建卡片容器
        colors = CardDefaults.cardColors( // 设置卡片颜色
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) // 使用半透明的表面变体色，更精致
        ), // 卡片颜色配置结束
        modifier = Modifier.fillMaxWidth(), // 填满宽度
        shape = RoundedCornerShape(20.dp), // 设置20dp圆角，更现代
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // 添加轻微阴影，增加层次感
    ) { // Card内容开始
        Column( // 创建垂直布局
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), // 增大内边距至20dp水平和16dp垂直
            verticalArrangement = Arrangement.spacedBy(12.dp) // 设置子组件之间12dp垂直间距
        ) { // Column内容开始
            Row( // 创建水平布局排列标签和数值
                modifier = Modifier.fillMaxWidth(), // 填满宽度
                horizontalArrangement = Arrangement.SpaceBetween, // 两端对齐排列
                verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
            ) { // Row内容开始
                Text( // 显示阈值标签
                    label, // 标签文本
                    style = MaterialTheme.typography.bodyMedium, // 使用中正文排版样式
                    fontWeight = FontWeight.Medium, // 设置字体为中等粗细
                    color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
                ) // 标签Text结束
                
                Card( // 创建数值显示卡片
                    colors = CardDefaults.cardColors( // 设置卡片颜色
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) // 使用半透明主色容器色
                    ), // 卡片颜色配置结束
                    shape = RoundedCornerShape(8.dp) // 设置8dp圆角
                ) { // Card内容开始
                    Text( // 显示当前阈值数值
                        String.format("%.2f", threshold), // 使用字符串模板显示格式化后的阈值
                        style = MaterialTheme.typography.labelMedium, // 使用中等标签排版样式
                        fontWeight = FontWeight.SemiBold, // 设置字体为半粗体
                        color = MaterialTheme.colorScheme.primary, // 设置颜色为主题主色
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp) // 添加10dp水平和6dp垂直内边距
                    ) // 数值Text结束
                } // 数值卡片结束
            } // Row内容结束
            
            Slider( // 创建滑块组件
                value = threshold, // 绑定当前阈值
                onValueChange = onThresholdChange, // 值变化时调用回调函数
                valueRange = 0.0f..1.0f, // 设置值范围为0到1
                steps = 99, // 设置步数为99（共100个刻度）
                modifier = Modifier.padding(vertical = 4.dp) // 上下各添加4dp内边距
            ) // Slider结束
            
            Row( // 创建水平布局排列范围标签
                modifier = Modifier.fillMaxWidth(), // 填满宽度
                horizontalArrangement = Arrangement.SpaceBetween // 两端对齐排列
            ) { // Row内容开始
                Text( // 显示左侧标签
                    "宽松", // 标签文本
                    style = MaterialTheme.typography.labelSmall, // 使用小标签排版样式
                    color = MaterialTheme.colorScheme.outline // 设置颜色为轮廓色
                ) // 左侧标签Text结束
                
                Text( // 显示右侧标签
                    "严格", // 标签文本
                    style = MaterialTheme.typography.labelSmall, // 使用小标签排版样式
                    color = MaterialTheme.colorScheme.outline // 设置颜色为轮廓色
                ) // 右侧标签Text结束
            } // Row内容结束
        } // Column内容结束
    } // Card内容结束
} // ThresholdSlider函数结束

@Composable // 标记LogCard为可组合函数
fun LogCard(logs: List<String>) { // 定义日志卡片函数，接收日志字符串列表
    Card( // 创建卡片容器
        colors = CardDefaults.cardColors( // 设置卡片颜色
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) // 使用半透明的表面变体色，更精致
        ), // 卡片颜色配置结束
        modifier = Modifier.fillMaxWidth(), // 填满宽度
        shape = RoundedCornerShape(20.dp), // 设置20dp圆角，更现代
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // 添加轻微阴影，增加层次感
    ) { // Card内容开始
        Column( // 创建垂直布局
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), // 增大内边距至20dp水平和16dp垂直
            verticalArrangement = Arrangement.spacedBy(12.dp) // 设置子组件之间12dp垂直间距
        ) { // Column内容开始
            Row( // 创建水平布局排列标题和图标
                modifier = Modifier.fillMaxWidth(), // 填满宽度
                horizontalArrangement = Arrangement.SpaceBetween, // 两端对齐排列
                verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
            ) { // Row内容开始
                Text( // 显示日志标题
                    "运行日志", // 标题文本
                    style = MaterialTheme.typography.titleMedium, // 使用中标题排版样式
                    fontWeight = FontWeight.SemiBold, // 设置字体为半粗体
                    color = MaterialTheme.colorScheme.primary // 设置颜色为主题主色
                ) // 标题Text结束
                
                Card( // 创建日志数量指示器卡片
                    colors = CardDefaults.cardColors( // 设置卡片颜色
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) // 使用半透明主色容器色
                    ), // 卡片颜色配置结束
                    shape = RoundedCornerShape(8.dp) // 设置8dp圆角
                ) { // Card内容开始
                    Text( // 显示日志数量
                        "${logs.size} 条", // 使用字符串模板显示日志数量
                        style = MaterialTheme.typography.labelSmall, // 使用小标签排版样式
                        fontWeight = FontWeight.Medium, // 设置字体为中等粗细
                        color = MaterialTheme.colorScheme.primary, // 设置颜色为主题主色
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp) // 添加8dp水平和4dp垂直内边距
                    ) // 数量Text结束
                } // 指示器卡片结束
            } // Row内容结束
            
            Card( // 创建日志内容容器卡片
                colors = CardDefaults.cardColors( // 设置卡片颜色
                    containerColor = MaterialTheme.colorScheme.surface // 使用表面色作为背景，更清晰
                ), // 卡片颜色配置结束
                modifier = Modifier // 开始链式修饰符
                    .fillMaxWidth() // 填满宽度
                    .height(220.dp), // 增大高度至220dp，显示更多日志
                shape = RoundedCornerShape(12.dp) // 设置12dp圆角
            ) { // Card内容开始
                LazyColumn( // 创建可滚动的日志列表
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp) // 添加12dp水平和8dp垂直内边距
                ) { // LazyColumn内容开始
                    items(logs.reversed()) { log -> // 遍历反转后的日志列表（最新的在上方显示）
                        Card( // 为每条日志创建卡片容器
                            colors = CardDefaults.cardColors( // 设置卡片颜色
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // 使用半透明的表面变体色
                            ), // 卡片颜色配置结束
                            modifier = Modifier // 开始链式修饰符
                                .fillMaxWidth() // 填满宽度
                                .padding(vertical = 2.dp), // 上下各添加2dp内边距
                            shape = RoundedCornerShape(8.dp) // 设置8dp圆角
                        ) { // Card内容开始
                            Text( // 显示单条日志
                                log, // 日志文本内容
                                style = MaterialTheme.typography.bodySmall, // 使用小正文排版样式
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), // 添加10dp水平和6dp垂直内边距
                                color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
                            ) // Text结束
                        } // 日志卡片结束
                    } // 日志遍历结束
                } // LazyColumn内容结束
            } // 日志容器Card结束
        } // Column内容结束
    } // Card内容结束
} // LogCard函数结束

// --- 新版主页组件（参照视频识别界面风格） ---

// 定义图片源卡片组件（参照VideoSourceCard风格）
@Composable
fun ImageSourceCard(
    imageUri: Uri?,
    resultBitmap: android.graphics.Bitmap?,
    onSelectImage: () -> Unit,
    onReselectImage: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "图片源",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(12.dp))
            
            if (imageUri == null && resultBitmap == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onSelectImage() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "选择图片文件开始识别",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                Button(
                    onClick = onSelectImage,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("选择图片")
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (resultBitmap != null) {
                        Image(
                            bitmap = resultBitmap.asImageBitmap(),
                            contentDescription = "识别结果",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "预览图片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onReselectImage,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("重新选择")
                }
            }
        }
    }
}

// 定义识别设置卡片组件（参照VideoSettingsCard风格）
@Composable
fun RecognitionSettingsCard(
    threshold: Float,
    onThresholdChange: (Float) -> Unit,
    detectionThreshold: Float,
    onDetectionThresholdChange: (Float) -> Unit,
    templateName: String?,
    onLoadTemplate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "识别设置",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onLoadTemplate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(templateName ?: "加载模板", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("相似度阈值", style = MaterialTheme.typography.bodyMedium)
                Text(String.format("%.2f", threshold), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            
            Slider(
                value = threshold,
                onValueChange = onThresholdChange,
                valueRange = 0.0f..1.0f,
                steps = 99,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Spacer(Modifier.height(4.dp))
            
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("检测阈值", style = MaterialTheme.typography.bodyMedium)
                Text(String.format("%.2f", detectionThreshold), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            
            Slider(
                value = detectionThreshold,
                onValueChange = onDetectionThresholdChange,
                valueRange = 0.0f..1.0f,
                steps = 99,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

// 定义处理状态显示组件（参照VideoProcessingStatus风格）
@Composable
fun ProcessingStatus(
    isProcessing: Boolean,
    statusMessage: String,
    isReady: Boolean
) {
    if (isProcessing || !isReady) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isProcessing)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(
                            if (isReady) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        statusMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// 定义图片控制按钮组件（参照VideoControlButtons风格）
@Composable
fun ImageControlButtons(
    isProcessing: Boolean,
    hasImage: Boolean,
    hasResult: Boolean,
    onStartRecognition: () -> Unit,
    onSaveImage: () -> Unit,
    onBatchRecognize: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isProcessing) {
                Button(
                    onClick = onStartRecognition,
                    enabled = hasImage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("开始识别")
                }
                
                OutlinedButton(
                    onClick = onBatchRecognize,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("批量识别")
                }
            } else {
                // 处理中：显示进度指示器（图片处理很快，无需取消按钮）
                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("识别中...")
                }
            }
        }
        
        // 有结果时显示保存按钮（单独一行）
        if (hasResult && !isProcessing) {
            Button(
                onClick = onSaveImage,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("保存结果")
            }
        }
    }
}
