package com.Enco.facerecognition.ui.components // 声明包名

import androidx.compose.animation.core.LinearEasing // 导入线性缓动
import androidx.compose.animation.core.RepeatMode // 导入重复模式
import androidx.compose.animation.core.animateFloat // 导入浮点动画
import androidx.compose.animation.core.infiniteRepeatable // 导入无限重复动画
import androidx.compose.animation.core.rememberInfiniteTransition // 导入无限过渡动画
import androidx.compose.animation.core.tween // 导入补间动画
import androidx.compose.foundation.layout.Box // 导入Box布局
import androidx.compose.foundation.layout.Column // 导入Column布局
import androidx.compose.foundation.layout.Row // 导入Row布局
import androidx.compose.foundation.layout.Spacer // 导入Spacer
import androidx.compose.foundation.layout.fillMaxWidth // 导入fillMaxWidth
import androidx.compose.foundation.layout.height // 导入height
import androidx.compose.foundation.layout.padding // 导入padding
import androidx.compose.foundation.layout.size // 导入size
import androidx.compose.foundation.layout.width // 导入width
import androidx.compose.material3.MaterialTheme // 导入Material主题
import androidx.compose.runtime.Composable // 导入Composable注解
import androidx.compose.runtime.getValue // 导入getValue委托
import androidx.compose.ui.Modifier // 导入Modifier
import androidx.compose.ui.draw.clip // 导入clip修饰符
import androidx.compose.ui.draw.drawBehind // 导入drawBehind修饰符
import androidx.compose.ui.graphics.Brush // 导入Brush
import androidx.compose.ui.graphics.Color // 导入Color
import androidx.compose.ui.unit.dp // 导入dp单位

// 骨架屏加载组件
@Composable
fun SkeletonLoader(
    modifier: Modifier = Modifier,
    height: Int = 20,
    width: Int = 0 // 0表示填满宽度
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonTranslate"
    )
    
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
    
    Box(
        modifier = modifier
            .then(
                if (width > 0) Modifier.width(width.dp)
                else Modifier.fillMaxWidth()
            )
            .height(height.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .drawBehind {
                val brush = Brush.horizontalGradient(
                    colors = shimmerColors,
                    startX = translateAnim - 1000f,
                    endX = translateAnim
                )
                drawRect(brush = brush)
            }
    )
}

// 骨架屏：状态卡片加载
@Composable
fun StatusCardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        SkeletonLoader(height = 24, width = 100)
        Spacer(Modifier.height(8.dp))
        SkeletonLoader(height = 16, width = 200)
    }
}

// 骨架屏：图片预览加载
@Composable
fun ImagePreviewSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
    ) {
        SkeletonLoader(height = 360)
    }
}

// 骨架屏：控制面板加载
@Composable
fun ControlPanelSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        SkeletonLoader(height = 48)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            SkeletonLoader(modifier = Modifier.weight(1f), height = 48)
            SkeletonLoader(modifier = Modifier.weight(1f), height = 48)
        }
        SkeletonLoader(height = 48)
    }
}
