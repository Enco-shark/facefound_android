package com.Enco.facefound.ui.theme // 声明包名为UI主题模块

import android.app.Activity // 导入Android Activity类
import android.os.Build // 导入Android Build类，用于版本判断
import androidx.compose.foundation.isSystemInDarkTheme // 导入系统深色主题检测函数
import androidx.compose.material3.MaterialTheme // 导入Material3主题组件
import androidx.compose.material3.darkColorScheme // 导入深色配色方案
import androidx.compose.material3.dynamicDarkColorScheme // 导入动态深色配色方案(Android 12+)
import androidx.compose.material3.dynamicLightColorScheme // 导入动态浅色配色方案(Android 12+)
import androidx.compose.material3.lightColorScheme // 导入浅色配色方案
import androidx.compose.runtime.Composable // 导入Compose可组合注解
import androidx.compose.runtime.SideEffect // 导入SideEffect副作用函数
import androidx.compose.ui.platform.LocalContext // 导入本地上下文
import androidx.compose.ui.platform.LocalView // 导入本地视图
import androidx.core.view.WindowCompat // 导入窗口兼容工具
import com.Enco.facefound.ui.theme.BackgroundDark // 导入深色背景色
import com.Enco.facefound.ui.theme.BackgroundLight // 导入浅色背景色
import com.Enco.facefound.ui.theme.OnBackgroundDark // 导入深色背景上的文字色
import com.Enco.facefound.ui.theme.OnBackgroundLight // 导入浅色背景上的文字色
import com.Enco.facefound.ui.theme.OnPrimaryDark // 导入深色主色上的文字色
import com.Enco.facefound.ui.theme.OnPrimaryLight // 导入浅色主色上的文字色
import com.Enco.facefound.ui.theme.OnSecondaryLight // 导入浅色次要色上的文字色
import com.Enco.facefound.ui.theme.OnSurfaceDark // 导入深色表面上的文字色
import com.Enco.facefound.ui.theme.OnSurfaceLight // 导入浅色表面上的文字色
import com.Enco.facefound.ui.theme.PrimaryDark // 导入深色主题主色
import com.Enco.facefound.ui.theme.PrimaryDarkBg // 导入深色主题主色容器色
import com.Enco.facefound.ui.theme.PrimaryLight // 导入浅色主题主色
import com.Enco.facefound.ui.theme.SecondaryLight // 导入浅色主题次要色
import com.Enco.facefound.ui.theme.SurfaceDark // 导入深色表面色
import com.Enco.facefound.ui.theme.SurfaceLight // 导入浅色表面色
import com.Enco.facefound.ui.theme.SurfaceVariantDark // 导入深色次表面色
import com.Enco.facefound.ui.theme.SurfaceVariantLight // 导入浅色次表面色

// 深色主题配色方案 —— 使用现代蓝紫暗色系
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,           // 主色：淡紫
    onPrimary = OnPrimaryDark,       // 主色上的文字/图标：深蓝黑
    primaryContainer = PrimaryDarkBg, // 主色容器：深蓝紫背景（按钮背景）
    onPrimaryContainer = PrimaryDark, // 主色容器上的文字：淡紫
    secondary = SurfaceVariantDark,    // 次要色：深次表面
    onSecondary = OnSurfaceDark,      // 次要色上的文字
    background = BackgroundDark,      // 页面背景：深蓝黑
    onBackground = OnBackgroundDark,  // 背景上的文字：淡紫白
    surface = SurfaceDark,             // 表面（卡片）：深蓝灰
    onSurface = OnSurfaceDark,        // 表面上的文字
    surfaceVariant = SurfaceVariantDark, // 次表面（列表等）
    onSurfaceVariant = OnSurfaceVariantDark, // 次表面上的文字
)

// 浅色主题配色方案 —— 现代蓝紫浅色系，极淡紫白背景
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,           // 主色：蓝紫
    onPrimary = OnPrimaryLight,       // 主色上的文字：白色
    primaryContainer = SecondaryLight, // 主色容器：淡紫灰背景
    onPrimaryContainer = PrimaryLight, // 主色容器上的文字：蓝紫
    secondary = SecondaryLight,        // 次要色：淡紫灰
    onSecondary = OnSecondaryLight,   // 次要色上的文字：蓝紫
    background = BackgroundLight,      // 页面背景：极淡紫白
    onBackground = OnBackgroundLight, // 背景上的文字：深蓝黑
    surface = SurfaceLight,           // 表面（卡片）：纯白
    onSurface = OnSurfaceLight,       // 表面上的文字：深蓝黑
    surfaceVariant = SurfaceVariantLight, // 次表面：淡紫灰
    onSurfaceVariant = OnSurfaceVariantLight, // 次表面上的文字：中灰
)

@Composable // 标记为Compose可组合函数
fun FaceRecognitionTheme( // 定义人脸识别应用主题函数
    darkTheme: Boolean = isSystemInDarkTheme(), // 是否深色主题，默认跟随系统
    dynamicColor: Boolean = false, // 是否使用动态颜色，默认关闭（使用自定义配色）
    content: @Composable () -> Unit // 主题包裹的内容lambda
) {
    val colorScheme = when { // 根据条件选择配色方案
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { // 动态颜色且Android 12+
            val context = LocalContext.current // 获取当前上下文
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) // 根据深浅色选择动态配色
        }
        darkTheme -> DarkColorScheme // 深色主题使用自定义深色配色
        else -> LightColorScheme // 浅色主题使用自定义浅色配色
    }

    val view = LocalView.current // 获取当前Compose视图
    if (!view.isInEditMode) { // 非预览模式时才执行
        SideEffect { // 使用SideEffect在组合后执行副作用
            val window = (view.context as Activity).window // 从视图上下文获取Activity窗口
            // 使用 WindowCompat 设置状态栏颜色，兼容 Android 15+
            WindowCompat.getInsetsController(window, view).apply { // 获取窗口insets控制器
                isAppearanceLightStatusBars = !darkTheme // 浅色主题时状态栏使用深色图标
                isAppearanceLightNavigationBars = !darkTheme // 浅色主题时导航栏使用深色图标
            }
        }
    }

    MaterialTheme( // 应用Material3主题
        colorScheme = colorScheme, // 设置配色方案
        typography = Typography, // 设置排版样式
        content = content // 渲染子内容
    )
}
