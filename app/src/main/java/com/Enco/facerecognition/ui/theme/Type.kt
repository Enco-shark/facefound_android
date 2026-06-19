package com.Enco.facerecognition.ui.theme // 声明包名为UI主题模块

import androidx.compose.material3.Typography // 导入Material3排版系统
import androidx.compose.ui.text.TextStyle // 导入文本样式类
import androidx.compose.ui.text.font.FontFamily // 导入字体族类
import androidx.compose.ui.text.font.FontWeight // 导入字体粗细类
import androidx.compose.ui.unit.sp // 导入sp缩放像素单位

val Typography = Typography( // 创建Material3排版实例
    bodyLarge = TextStyle( // 定义正文大号样式
        fontFamily = FontFamily.Default, // 使用系统默认字体
        fontWeight = FontWeight.Normal, // 正常字体粗细
        fontSize = 16.sp, // 字号16sp
        lineHeight = 24.sp, // 行高24sp
        letterSpacing = 0.5.sp // 字间距0.5sp
    ),
    titleLarge = TextStyle( // 定义标题大号样式
        fontFamily = FontFamily.Default, // 使用系统默认字体
        fontWeight = FontWeight.Normal, // 正常字体粗细
        fontSize = 22.sp, // 字号22sp
        lineHeight = 28.sp, // 行高28sp
        letterSpacing = 0.sp // 字间距0sp
    ),
    labelSmall = TextStyle( // 定义标签小号样式
        fontFamily = FontFamily.Default, // 使用系统默认字体
        fontWeight = FontWeight.Medium, // 中等字体粗细
        fontSize = 11.sp, // 字号11sp
        lineHeight = 16.sp, // 行高16sp
        letterSpacing = 0.5.sp // 字间距0.5sp
    )
)
