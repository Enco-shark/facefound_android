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

private val DarkColorScheme = darkColorScheme( // 定义深色主题配色方案
    primary = Purple80, // 主色使用深色主题紫色
    secondary = PurpleGrey80, // 次要色使用深色主题灰紫色
    tertiary = Pink80 // 第三色使用深色主题粉红色
)

private val LightColorScheme = lightColorScheme( // 定义浅色主题配色方案
    primary = Purple40, // 主色使用浅色主题紫色
    secondary = PurpleGrey40, // 次要色使用浅色主题灰紫色
    tertiary = Pink40 // 第三色使用浅色主题粉红色
)

@Composable // 标记为Compose可组合函数
fun FaceRecognitionTheme( // 定义人脸识别应用主题函数
    darkTheme: Boolean = isSystemInDarkTheme(), // 是否深色主题，默认跟随系统
    dynamicColor: Boolean = true, // 是否使用动态颜色，默认开启(Android 12+)
    content: @Composable () -> Unit // 主题包裹的内容lambda
) {
    val colorScheme = when { // 根据条件选择配色方案
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { // 动态颜色且Android 12+
            val context = LocalContext.current // 获取当前上下文
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) // 根据深浅色选择动态配色
        }
        darkTheme -> DarkColorScheme // 深色主题使用静态深色配色
        else -> LightColorScheme // 浅色主题使用静态浅色配色
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
