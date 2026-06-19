package com.Enco.facefound.ui.theme // 声明包名为UI主题模块

import androidx.compose.ui.graphics.Color // 导入Compose颜色类

// === 浅色主题（默认）===
// 主色：现代蓝紫色，更有科技感
val PrimaryLight = Color(0xFF6C63FF)       // 主色：蓝紫渐变起点
val PrimaryDarkLight = Color(0xFF5A52F3)   // 主色深：蓝紫渐变终点
val OnPrimaryLight = Color(0xFFFFFFFF)        // 主色上的文字/图标色（白色）

// 次要色：淡紫灰
val SecondaryLight = Color(0xFFE8E5FF)     // 次要容器背景
val OnSecondaryLight = Color(0xFF6C63FF)    // 次要容器上的文字色

// 表面色：纯白 + 淡灰
val SurfaceLight = Color(0xFFFFFFFF)          // 主表面（卡片背景）
val SurfaceVariantLight = Color(0xFFF5F5F8) // 次表面（列表背景）
val OnSurfaceLight = Color(0xFF1A1A2E)      // 主文字色（深蓝黑）
val OnSurfaceVariantLight = Color(0xFF6B6B80) // 次文字色（中灰）

// 背景色
val BackgroundLight = Color(0xFFF8F7FF)      // 页面背景（极淡紫白）
val OnBackgroundLight = Color(0xFF1A1A2E)    // 背景上的文字色

// === 深色主题 ===
val PrimaryDark = Color(0xFF9D95FF)         // 主色：淡紫
val PrimaryDarkBg = Color(0xFF2D2B55)       // 主色容器（按钮背景）
val OnPrimaryDark = Color(0xFF1A1A2E)       // 主色上的文字色

val SurfaceDark = Color(0xFF1E1E2E)         // 表面：深蓝灰
val SurfaceVariantDark = Color(0xFF2A2A3E)   // 次表面
val OnSurfaceDark = Color(0xFFE8E8F0)       // 主文字色（淡紫白）
val OnSurfaceVariantDark = Color(0xFF9B9BAC) // 次文字色

val BackgroundDark = Color(0xFF12121E)        // 页面背景（深蓝黑）
val OnBackgroundDark = Color(0xFFE8E8F0)     // 背景上的文字色

// === 功能色 ===
val Success = Color(0xFF00C853)              // 成功绿
val Warning = Color(0xFFFF9100)              // 警告橙
val Error = Color(0xFFFF5252)                // 错误红
val Info = Color(0xFF448AFF)                // 信息蓝

// === 渐变辅助色 ===
val GradientStart = Color(0xFF6C63FF)        // 渐变起点
val GradientEnd = Color(0xFF48C6EF)          // 渐变终点（青蓝）
