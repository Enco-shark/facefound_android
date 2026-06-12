package com.Enco.facefound.ui.screens // 声明当前文件所属的包路径，用于关于屏幕

import android.content.Intent // 导入Intent类，用于打开链接
import android.net.Uri // 导入Uri类，用于表示链接地址
import androidx.compose.foundation.layout.Arrangement // 导入排列方式，用于控制子组件的间距和对齐
import androidx.compose.foundation.layout.Box // 导入Box布局组件，用于层叠放置子组件
import androidx.compose.foundation.layout.Column // 导入Column布局组件，用于垂直排列子组件
import androidx.compose.foundation.layout.Row // 导入Row布局组件，用于水平排列子组件
import androidx.compose.foundation.layout.Spacer // 导入Spacer组件，用于在布局中创建空白间距
import androidx.compose.foundation.layout.fillMaxWidth // 导入fillMaxWidth修饰符，使组件填满父容器的宽度
import androidx.compose.foundation.layout.height // 导入height修饰符，用于设置组件的固定高度
import androidx.compose.foundation.layout.padding // 导入padding修饰符，用于为组件添加内边距
import androidx.compose.foundation.layout.size // 导入size修饰符，用于同时设置组件的宽高
import androidx.compose.foundation.layout.width // 导入width修饰符，用于设置组件的固定宽度
import androidx.compose.foundation.lazy.LazyColumn // 导入懒加载列表组件
import androidx.compose.foundation.shape.RoundedCornerShape // 导入圆角矩形形状
import androidx.compose.material.icons.Icons // 导入Material图标集合
import androidx.compose.material.icons.automirrored.filled.OpenInNew // 导入自动镜像的外部链接图标
import androidx.compose.material.icons.filled.AccountCircle // 导入账户圆形图标
import androidx.compose.material.icons.filled.BugReport // 导入Bug报告图标
import androidx.compose.material.icons.filled.Code // 导入代码图标
import androidx.compose.material.icons.filled.Description // 导入文档图标
import androidx.compose.material.icons.filled.Extension // 导入扩展图标
import androidx.compose.material.icons.filled.Group // 导入群组图标
import androidx.compose.material.icons.filled.Link // 导入链接图标
import androidx.compose.material.icons.filled.Memory // 导入内存图标
import androidx.compose.material.icons.filled.People // 导入人群图标
import androidx.compose.material.icons.filled.Person // 导入人物图标
import androidx.compose.material3.Card // 导入卡片组件
import androidx.compose.material3.CardDefaults // 导入卡片默认配置
import androidx.compose.material3.HorizontalDivider // 导入水平分割线组件
import androidx.compose.material3.Icon // 导入图标组件
import androidx.compose.material3.IconButton // 导入图标按钮组件
import androidx.compose.material3.MaterialTheme // 导入Material主题对象
import androidx.compose.material3.SuggestionChip // 导入建议芯片组件
import androidx.compose.material3.Text // 导入文本组件
import androidx.compose.runtime.Composable // 导入Composable注解
import androidx.compose.ui.Alignment // 导入对齐方式
import androidx.compose.ui.Modifier // 导入修饰符
import androidx.compose.ui.graphics.vector.ImageVector // 导入图标矢量类
import androidx.compose.ui.platform.LocalContext // 导入本地上下文
import androidx.compose.ui.text.font.FontWeight // 导入字体粗细
import androidx.compose.ui.text.style.TextAlign // 导入文本对齐方式
import androidx.compose.ui.unit.dp // 导入dp单位
import com.Enco.facefound.BuildConfig // 导入构建配置

@Composable // 标记AboutScreen为可组合函数
fun AboutScreen( // 定义关于屏幕函数
    modifier: Modifier = Modifier // 接收外部修饰符，默认为空Modifier
) { // 函数体开始
    val context = LocalContext.current // 获取当前上下文，用于启动Intent

    LazyColumn( // 创建可滚动的垂直懒加载列表
        modifier = modifier.padding(16.dp), // 应用外部修饰符并添加16dp内边距
        verticalArrangement = Arrangement.spacedBy(16.dp) // 子项之间设置16dp的垂直间距
    ) { // LazyColumn内容区域开始
        // 应用信息卡片
        item { // 列表项：应用信息卡片
            AppInfoCard() // 渲染应用信息卡片
        } // 应用信息卡片项结束

        // 项目开发人员
        item { // 列表项：开发人员卡片
            DevelopersCard() // 渲染开发人员卡片
        } // 开发人员卡片项结束

        // 核心依赖致谢
        item { // 列表项：核心依赖卡片
            CoreDependenciesCard() // 渲染核心依赖卡片
        } // 核心依赖卡片项结束

        // 项目链接
        item { // 列表项：项目链接卡片
            ProjectLinksCard( // 渲染项目链接卡片
                onLinkClick = { url -> // 定义链接点击回调
                    try { // 尝试打开链接，防止无浏览器时崩溃
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)) // 创建打开链接的Intent
                        context.startActivity(intent) // 启动Intent打开链接
                    } catch (e: Exception) { // 捕获无浏览器或ActivityNotFoundException等异常
                        android.util.Log.e("AboutScreen", "无法打开链接: ${e.message}") // 记录错误日志
                    } // 结束异常捕获
                } // 链接点击回调结束
            ) // ProjectLinksCard调用结束
        } // 项目链接卡片项结束

        // 许可证信息
        item { // 列表项：许可证卡片
            LicenseCard() // 渲染许可证卡片
        } // 许可证卡片项结束
    } // LazyColumn内容区域结束
} // AboutScreen函数结束

@Composable // 标记AppInfoCard为可组合函数
private fun AppInfoCard() { // 定义应用信息卡片函数
    Card( // 创建卡片容器
        modifier = Modifier.fillMaxWidth(), // 填满宽度
        shape = RoundedCornerShape(16.dp), // 设置16dp圆角
        colors = CardDefaults.cardColors( // 设置卡片颜色
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) // 使用半透明主色容器色
        ) // 卡片颜色配置结束
    ) { // Card内容开始
        Column( // 创建垂直布局
            modifier = Modifier // 开始链式修饰符
                .fillMaxWidth() // 填满宽度
                .padding(24.dp), // 添加24dp内边距
            horizontalAlignment = Alignment.CenterHorizontally // 水平居中对齐
        ) { // Column内容开始
            // 应用图标
            Box( // 创建图标容器Box
                modifier = Modifier // 开始链式修饰符
                    .size(80.dp) // 设置大小为80dp
                    .padding(bottom = 16.dp), // 底部添加16dp内边距
                contentAlignment = Alignment.Center // 内容居中对齐
            ) { // Box内容开始
                Icon( // 显示应用图标
                    imageVector = Icons.Default.AccountCircle, // 使用账户圆形图标
                    contentDescription = "应用图标", // 无障碍描述
                    modifier = Modifier.size(64.dp), // 设置图标大小为64dp
                    tint = MaterialTheme.colorScheme.primary // 设置颜色为主题主色
                ) // Icon结束
            } // Box内容结束

            // 应用名称
            Text( // 显示应用名称
                text = "FaceFound", // 文本内容
                style = MaterialTheme.typography.headlineMedium, // 使用中标题排版样式
                fontWeight = FontWeight.Bold, // 设置字体为粗体
                textAlign = TextAlign.Center // 文本居中对齐
            ) // 应用名称Text结束

            Spacer(Modifier.height(8.dp)) // 创建8dp垂直间距

            // 版本号
            Text( // 显示版本号
                text = "v${BuildConfig.VERSION_NAME}", // 使用字符串模板拼接版本号
                style = MaterialTheme.typography.bodyLarge, // 使用大正文排版样式
                color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
            ) // 版本号Text结束

            Spacer(Modifier.height(12.dp)) // 创建12dp垂直间距

            // 应用描述
            Text( // 显示应用描述
                text = "基于 InsightFace 和 ONNX Runtime 的离线人脸识别应用", // 描述文本
                style = MaterialTheme.typography.bodyMedium, // 使用中正文排版样式
                textAlign = TextAlign.Center, // 文本居中对齐
                color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
            ) // 应用描述Text结束
        } // Column内容结束
    } // Card内容结束
} // AppInfoCard函数结束

@Composable // 标记DevelopersCard为可组合函数
private fun DevelopersCard() { // 定义开发人员卡片函数
    Card( // 创建卡片容器
        modifier = Modifier.fillMaxWidth(), // 填满宽度
        shape = RoundedCornerShape(14.dp), // 设置14dp圆角
        colors = CardDefaults.cardColors( // 设置卡片颜色
            containerColor = MaterialTheme.colorScheme.surfaceVariant // 使用表面变体色作为背景
        ) // 卡片颜色配置结束
    ) { // Card内容开始
        Column( // 创建垂直布局
            modifier = Modifier.padding(16.dp) // 添加16dp内边距
        ) { // Column内容开始
            // 标题行
            Row( // 创建水平布局排列图标和标题
                verticalAlignment = Alignment.CenterVertically, // 垂直居中对齐
                modifier = Modifier.padding(bottom = 16.dp) // 底部添加16dp内边距
            ) { // Row内容开始
                Icon( // 显示开发人员图标
                    imageVector = Icons.Default.People, // 使用人群图标
                    contentDescription = null, // 无障碍描述为空
                    tint = MaterialTheme.colorScheme.primary, // 设置颜色为主题主色
                    modifier = Modifier.size(24.dp) // 设置图标大小为24dp
                ) // Icon结束
                Spacer(Modifier.width(12.dp)) // 创建12dp水平间距
                Text( // 显示标题文本
                    text = "项目开发人员", // 标题文本
                    style = MaterialTheme.typography.titleMedium, // 使用中标题排版样式
                    fontWeight = FontWeight.SemiBold // 设置字体为半粗体
                ) // 标题Text结束
            } // Row标题行结束

            // 开发人员列表
            DeveloperItem( // 渲染开发人员项
                name = "Enco", // 开发人员名称
                role = "项目负责人 & 核心开发", // 角色描述
                icon = Icons.Default.Person // 使用人物图标
            ) // 开发人员项调用结束

            HorizontalDivider( // 显示水平分割线
                modifier = Modifier.padding(vertical = 8.dp), // 上下各留8dp间距
                color = MaterialTheme.colorScheme.outlineVariant // 设置分割线颜色
            ) // HorizontalDivider结束

            DeveloperItem( // 渲染开发人员项
                name = "FaceFound 团队", // 开发人员名称
                role = "UI设计 & 测试", // 角色描述
                icon = Icons.Default.Group // 使用群组图标
            ) // 开发人员项调用结束
        } // Column内容结束
    } // Card内容结束
} // DevelopersCard函数结束

@Composable // 标记DeveloperItem为可组合函数
private fun DeveloperItem( // 定义开发人员项函数
    name: String, // 开发人员名称参数
    role: String, // 角色描述参数
    icon: ImageVector // 图标参数
) { // 函数体开始
    Row( // 创建水平布局排列图标和信息
        modifier = Modifier // 开始链式修饰符
            .fillMaxWidth() // 填满宽度
            .padding(vertical = 8.dp), // 上下各添加8dp内边距
        verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
    ) { // Row内容开始
        // 头像图标
        Box( // 创建头像容器Box
            modifier = Modifier // 开始链式修饰符
                .size(48.dp) // 设置大小为48dp
                .padding(end = 12.dp), // 右侧添加12dp内边距
            contentAlignment = Alignment.Center // 内容居中对齐
        ) { // Box内容开始
            Icon( // 显示头像图标
                imageVector = icon, // 传入图标
                contentDescription = null, // 无障碍描述为空
                modifier = Modifier.size(32.dp), // 设置图标大小为32dp
                tint = MaterialTheme.colorScheme.primary // 设置颜色为主题主色
            ) // Icon结束
        } // Box头像容器结束

        // 信息区域
        Column( // 创建垂直布局排列名称和角色
            modifier = Modifier.weight(1f) // 占据剩余可用宽度
        ) { // Column内容开始
            Text( // 显示开发人员名称
                text = name, // 文本内容为名称
                style = MaterialTheme.typography.bodyLarge, // 使用大正文排版样式
                fontWeight = FontWeight.Medium // 设置字体为中等粗细
            ) // 名称Text结束
            Text( // 显示角色描述
                text = role, // 文本内容为角色
                style = MaterialTheme.typography.bodySmall, // 使用小正文排版样式
                color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
            ) // 角色Text结束
        } // Column信息区域结束
    } // Row内容结束
} // DeveloperItem函数结束

@Composable // 标记CoreDependenciesCard为可组合函数
private fun CoreDependenciesCard() { // 定义核心依赖卡片函数
    Card( // 创建卡片容器
        modifier = Modifier.fillMaxWidth(), // 填满宽度
        shape = RoundedCornerShape(14.dp), // 设置14dp圆角
        colors = CardDefaults.cardColors( // 设置卡片颜色
            containerColor = MaterialTheme.colorScheme.surfaceVariant // 使用表面变体色作为背景
        ) // 卡片颜色配置结束
    ) { // Card内容开始
        Column( // 创建垂直布局
            modifier = Modifier.padding(16.dp) // 添加16dp内边距
        ) { // Column内容开始
            // 标题行
            Row( // 创建水平布局排列图标和标题
                verticalAlignment = Alignment.CenterVertically, // 垂直居中对齐
                modifier = Modifier.padding(bottom = 16.dp) // 底部添加16dp内边距
            ) { // Row内容开始
                Icon( // 显示依赖图标
                    imageVector = Icons.Default.Extension, // 使用扩展图标
                    contentDescription = null, // 无障碍描述为空
                    tint = MaterialTheme.colorScheme.primary, // 设置颜色为主题主色
                    modifier = Modifier.size(24.dp) // 设置图标大小为24dp
                ) // Icon结束
                Spacer(Modifier.width(12.dp)) // 创建12dp水平间距
                Text( // 显示标题文本
                    text = "核心依赖致谢", // 标题文本
                    style = MaterialTheme.typography.titleMedium, // 使用中标题排版样式
                    fontWeight = FontWeight.SemiBold // 设置字体为半粗体
                ) // 标题Text结束
            } // Row标题行结束

            // 依赖列表
            DependencyItem( // 渲染依赖项
                name = "ONNX Runtime", // 依赖名称
                version = "1.17.0", // 版本号
                description = "微软开源的跨平台机器学习推理引擎", // 描述
                license = "MIT License" // 许可证类型
            ) // 依赖项调用结束

            HorizontalDivider( // 显示水平分割线
                modifier = Modifier.padding(vertical = 8.dp), // 上下各留8dp间距
                color = MaterialTheme.colorScheme.outlineVariant // 设置分割线颜色
            ) // HorizontalDivider结束

            DependencyItem( // 渲染依赖项
                name = "InsightFace", // 依赖名称
                version = "buffalo_l", // 版本号
                description = "开源人脸分析工具包，提供检测、识别、对齐模型", // 描述
                license = "MIT License" // 许可证类型
            ) // 依赖项调用结束

            HorizontalDivider( // 显示水平分割线
                modifier = Modifier.padding(vertical = 8.dp), // 上下各留8dp间距
                color = MaterialTheme.colorScheme.outlineVariant // 设置分割线颜色
            ) // HorizontalDivider结束

            DependencyItem( // 渲染依赖项
                name = "Jetpack Compose", // 依赖名称
                version = "BOM 2024.02.00", // 版本号
                description = "Android 现代声明式 UI 工具包", // 描述
                license = "Apache License 2.0" // 许可证类型
            ) // 依赖项调用结束

            HorizontalDivider( // 显示水平分割线
                modifier = Modifier.padding(vertical = 8.dp), // 上下各留8dp间距
                color = MaterialTheme.colorScheme.outlineVariant // 设置分割线颜色
            ) // HorizontalDivider结束

            DependencyItem( // 渲染依赖项
                name = "Kotlin Coroutines", // 依赖名称
                version = "1.7.3", // 版本号
                description = "Kotlin 协程库，支持异步编程", // 描述
                license = "Apache License 2.0" // 许可证类型
            ) // 依赖项调用结束
        } // Column内容结束
    } // Card内容结束
} // CoreDependenciesCard函数结束

@Composable // 标记DependencyItem为可组合函数
private fun DependencyItem( // 定义依赖项函数
    name: String, // 依赖名称参数
    version: String, // 版本号参数
    description: String, // 描述参数
    license: String // 许可证参数
) { // 函数体开始
    Column( // 创建垂直布局排列依赖信息
        modifier = Modifier // 开始链式修饰符
            .fillMaxWidth() // 填满宽度
            .padding(vertical = 8.dp) // 上下各添加8dp内边距
    ) { // Column内容开始
        // 名称和版本行
        Row( // 创建水平布局排列名称和版本
            modifier = Modifier.fillMaxWidth(), // 填满宽度
            horizontalArrangement = Arrangement.SpaceBetween, // 两端对齐排列
            verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
        ) { // Row内容开始
            Text( // 显示依赖名称
                text = name, // 文本内容为名称
                style = MaterialTheme.typography.bodyLarge, // 使用大正文排版样式
                fontWeight = FontWeight.Medium // 设置字体为中等粗细
            ) // 名称Text结束
            SuggestionChip( // 创建建议芯片显示版本号
                onClick = { }, // 点击事件为空
                label = { Text(version) } // 芯片标签为版本号
            ) // SuggestionChip结束
        } // Row名称版本行结束

        Spacer(Modifier.height(4.dp)) // 创建4dp垂直间距

        // 描述
        Text( // 显示依赖描述
            text = description, // 文本内容为描述
            style = MaterialTheme.typography.bodySmall, // 使用小正文排版样式
            color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
        ) // 描述Text结束

        Spacer(Modifier.height(4.dp)) // 创建4dp垂直间距

        // 许可证
        Text( // 显示许可证信息
            text = "许可证: $license", // 使用字符串模板拼接许可证
            style = MaterialTheme.typography.labelSmall, // 使用小标签排版样式
            color = MaterialTheme.colorScheme.outline // 设置颜色为轮廓色
        ) // 许可证Text结束
    } // Column内容结束
} // DependencyItem函数结束

@Composable // 标记ProjectLinksCard为可组合函数
private fun ProjectLinksCard( // 定义项目链接卡片函数
    onLinkClick: (String) -> Unit // 接收链接点击回调参数
) { // 函数体开始
    Card( // 创建卡片容器
        modifier = Modifier.fillMaxWidth(), // 填满宽度
        shape = RoundedCornerShape(14.dp), // 设置14dp圆角
        colors = CardDefaults.cardColors( // 设置卡片颜色
            containerColor = MaterialTheme.colorScheme.surfaceVariant // 使用表面变体色作为背景
        ) // 卡片颜色配置结束
    ) { // Card内容开始
        Column( // 创建垂直布局
            modifier = Modifier.padding(16.dp) // 添加16dp内边距
        ) { // Column内容开始
            // 标题行
            Row( // 创建水平布局排列图标和标题
                verticalAlignment = Alignment.CenterVertically, // 垂直居中对齐
                modifier = Modifier.padding(bottom = 16.dp) // 底部添加16dp内边距
            ) { // Row内容开始
                Icon( // 显示链接图标
                    imageVector = Icons.Default.Link, // 使用链接图标
                    contentDescription = null, // 无障碍描述为空
                    tint = MaterialTheme.colorScheme.primary, // 设置颜色为主题主色
                    modifier = Modifier.size(24.dp) // 设置图标大小为24dp
                ) // Icon结束
                Spacer(Modifier.width(12.dp)) // 创建12dp水平间距
                Text( // 显示标题文本
                    text = "项目链接", // 标题文本
                    style = MaterialTheme.typography.titleMedium, // 使用中标题排版样式
                    fontWeight = FontWeight.SemiBold // 设置字体为半粗体
                ) // 标题Text结束
            } // Row标题行结束

            // 链接列表
            LinkItem( // 渲染链接项
                title = "GitHub 仓库", // 链接标题
                subtitle = "项目源代码和文档", // 链接副标题
                icon = Icons.Default.Code, // 使用代码图标
                url = "https://github.com/Enco-shark/facefound_android", // 链接地址
                onClick = onLinkClick // 传入点击回调
            ) // 链接项调用结束

            HorizontalDivider( // 显示水平分割线
                modifier = Modifier.padding(vertical = 8.dp), // 上下各留8dp间距
                color = MaterialTheme.colorScheme.outlineVariant // 设置分割线颜色
            ) // HorizontalDivider结束

            LinkItem( // 渲染链接项
                title = "问题反馈", // 链接标题
                subtitle = "提交 Bug 报告或功能建议", // 链接副标题
                icon = Icons.Default.BugReport, // 使用Bug报告图标
                url = "https://github.com/Enco-shark/facefound_android/issues", // 链接地址
                onClick = onLinkClick // 传入点击回调
            ) // 链接项调用结束

            HorizontalDivider( // 显示水平分割线
                modifier = Modifier.padding(vertical = 8.dp), // 上下各留8dp间距
                color = MaterialTheme.colorScheme.outlineVariant // 设置分割线颜色
            ) // HorizontalDivider结束

            LinkItem( // 渲染链接项
                title = "InsightFace 官网", // 链接标题
                subtitle = "人脸分析研究项目", // 链接副标题
                icon = Icons.Default.AccountCircle, // 使用账户圆形图标
                url = "https://github.com/deepinsight/insightface", // 链接地址
                onClick = onLinkClick // 传入点击回调
            ) // 链接项调用结束

            HorizontalDivider( // 显示水平分割线
                modifier = Modifier.padding(vertical = 8.dp), // 上下各留8dp间距
                color = MaterialTheme.colorScheme.outlineVariant // 设置分割线颜色
            ) // HorizontalDivider结束

            LinkItem( // 渲染链接项
                title = "ONNX Runtime", // 链接标题
                subtitle = "跨平台机器学习推理引擎", // 链接副标题
                icon = Icons.Default.Memory, // 使用内存图标
                url = "https://github.com/microsoft/onnxruntime", // 链接地址
                onClick = onLinkClick // 传入点击回调
            ) // 链接项调用结束
        } // Column内容结束
    } // Card内容结束
} // ProjectLinksCard函数结束

@Composable // 标记LinkItem为可组合函数
private fun LinkItem( // 定义链接项函数
    title: String, // 链接标题参数
    subtitle: String, // 链接副标题参数
    icon: ImageVector, // 图标参数
    url: String, // 链接地址参数
    onClick: (String) -> Unit // 点击回调参数
) { // 函数体开始
    Row( // 创建水平布局排列图标、信息和箭头
        modifier = Modifier // 开始链式修饰符
            .fillMaxWidth() // 填满宽度
            .padding(vertical = 8.dp), // 上下各添加8dp内边距
        verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
    ) { // Row内容开始
        // 图标
        Box( // 创建图标容器Box
            modifier = Modifier // 开始链式修饰符
                .size(40.dp) // 设置大小为40dp
                .padding(end = 12.dp), // 右侧添加12dp内边距
            contentAlignment = Alignment.Center // 内容居中对齐
        ) { // Box内容开始
            Icon( // 显示链接图标
                imageVector = icon, // 传入图标
                contentDescription = null, // 无障碍描述为空
                modifier = Modifier.size(24.dp), // 设置图标大小为24dp
                tint = MaterialTheme.colorScheme.primary // 设置颜色为主题主色
            ) // Icon结束
        } // Box图标容器结束

        // 信息区域
        Column( // 创建垂直布局排列标题和副标题
            modifier = Modifier.weight(1f) // 占据剩余可用宽度
        ) { // Column内容开始
            Text( // 显示链接标题
                text = title, // 文本内容为标题
                style = MaterialTheme.typography.bodyLarge, // 使用大正文排版样式
                fontWeight = FontWeight.Medium // 设置字体为中等粗细
            ) // 标题Text结束
            Text( // 显示链接副标题
                text = subtitle, // 文本内容为副标题
                style = MaterialTheme.typography.bodySmall, // 使用小正文排版样式
                color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
            ) // 副标题Text结束
        } // Column信息区域结束

        // 箭头图标
        IconButton( // 创建图标按钮用于打开链接
            onClick = { onClick(url) } // 点击时调用回调打开链接
        ) { // IconButton内容开始
            Icon( // 显示外部链接图标
                imageVector = Icons.AutoMirrored.Filled.OpenInNew, // 使用自动镜像的在新窗口打开图标
                contentDescription = "打开链接", // 无障碍描述
                tint = MaterialTheme.colorScheme.outline // 设置颜色为轮廓色
            ) // Icon结束
        } // IconButton结束
    } // Row内容结束
} // LinkItem函数结束

@Composable // 标记LicenseCard为可组合函数
private fun LicenseCard() { // 定义许可证卡片函数
    Card( // 创建卡片容器
        modifier = Modifier.fillMaxWidth(), // 填满宽度
        shape = RoundedCornerShape(14.dp), // 设置14dp圆角
        colors = CardDefaults.cardColors( // 设置卡片颜色
            containerColor = MaterialTheme.colorScheme.surfaceVariant // 使用表面变体色作为背景
        ) // 卡片颜色配置结束
    ) { // Card内容开始
        Column( // 创建垂直布局
            modifier = Modifier.padding(16.dp) // 添加16dp内边距
        ) { // Column内容开始
            // 标题行
            Row( // 创建水平布局排列图标和标题
                verticalAlignment = Alignment.CenterVertically, // 垂直居中对齐
                modifier = Modifier.padding(bottom = 16.dp) // 底部添加16dp内边距
            ) { // Row内容开始
                Icon( // 显示许可证图标
                    imageVector = Icons.Default.Description, // 使用文档图标
                    contentDescription = null, // 无障碍描述为空
                    tint = MaterialTheme.colorScheme.primary, // 设置颜色为主题主色
                    modifier = Modifier.size(24.dp) // 设置图标大小为24dp
                ) // Icon结束
                Spacer(Modifier.width(12.dp)) // 创建12dp水平间距
                Text( // 显示标题文本
                    text = "许可证", // 标题文本
                    style = MaterialTheme.typography.titleMedium, // 使用中标题排版样式
                    fontWeight = FontWeight.SemiBold // 设置字体为半粗体
                ) // 标题Text结束
            } // Row标题行结束

            // 许可证内容
            Card( // 创建内部卡片容器
                modifier = Modifier.fillMaxWidth(), // 填满宽度
                shape = RoundedCornerShape(8.dp), // 设置8dp圆角
                colors = CardDefaults.cardColors( // 设置卡片颜色
                    containerColor = MaterialTheme.colorScheme.surface // 使用表面色作为背景
                ) // 卡片颜色配置结束
            ) { // Card内容开始
                Column( // 创建垂直布局
                    modifier = Modifier.padding(12.dp) // 添加12dp内边距
                ) { // Column内容开始
                    Text( // 显示项目许可证标题
                        text = "FaceFound - 人脸识别 Android 应用", // 文本内容
                        style = MaterialTheme.typography.bodyMedium, // 使用中正文排版样式
                        fontWeight = FontWeight.Bold // 设置字体为粗体
                    ) // 项目许可证标题Text结束

                    Spacer(Modifier.height(8.dp)) // 创建8dp垂直间距

                    Text( // 显示许可证正文
                        text = buildString { // 使用buildString构建许可证文本
                            appendLine("Copyright (c) 2024 Enco") // 版权声明
                            appendLine("") // 空行
                            appendLine("本项目基于 InsightFace 和 ONNX Runtime 构建，遵循各自的开源协议。") // 许可证说明
                            appendLine("") // 空行
                            appendLine("核心依赖:") // 核心依赖标题
                            appendLine("- ONNX Runtime: MIT License") // ONNX Runtime 许可证
                            appendLine("- InsightFace: MIT License") // InsightFace 许可证
                            appendLine("- Jetpack Compose: Apache License 2.0") // Jetpack Compose 许可证
                            appendLine("- Kotlin Coroutines: Apache License 2.0") // Kotlin Coroutines 许可证
                        }, // buildString结束
                        style = MaterialTheme.typography.bodySmall, // 使用小正文排版样式
                        color = MaterialTheme.colorScheme.onSurfaceVariant // 设置颜色为表面变体上的文字色
                    ) // 许可证正文Text结束
                } // Column内容结束
            } // 内部Card内容结束

            Spacer(Modifier.height(12.dp)) // 创建12dp垂直间距

            // 声明
            Text( // 显示声明文本
                text = "本应用为离线应用，所有推理均在设备端完成，不收集任何用户数据。", // 声明文本内容
                style = MaterialTheme.typography.labelSmall, // 使用小标签排版样式
                color = MaterialTheme.colorScheme.outline, // 设置颜色为轮廓色
                textAlign = TextAlign.Center, // 文本居中对齐
                modifier = Modifier.fillMaxWidth() // 填满宽度
            ) // 声明Text结束
        } // Column内容结束
    } // Card内容结束
} // LicenseCard函数结束