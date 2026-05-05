package com.Enco.facefound.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Enco.facefound.ui.viewmodel.FaceRecognitionViewModel

@Composable
fun VideoScreen(
    viewModel: FaceRecognitionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showVideoInfo by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setVideoUri(it) }
    }

    LaunchedEffect(uiState.videoProcessingState) {
        if (uiState.videoProcessingState is FaceRecognitionViewModel.VideoProcessingState.Completed ||
            uiState.videoProcessingState is FaceRecognitionViewModel.VideoProcessingState.Error) {
            showVideoInfo = true
        } else if (uiState.videoProcessingState is FaceRecognitionViewModel.VideoProcessingState.Idle &&
            uiState.videoUri != null) {
            showVideoInfo = true
        }
    }

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "视频识别",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            VideoSourceCard(
                videoUri = uiState.videoUri,
                videoInfo = uiState.videoInfo,
                onSelectVideo = { videoPicker.launch("video/*") }
            )
        }

        item {
            VideoSettingsCard(
                threshold = uiState.videoThreshold,
                onThresholdChange = { viewModel.updateVideoThreshold(it) },
                sampleRate = uiState.videoSampleRate,
                onSampleRateChange = { viewModel.updateVideoSampleRate(it) }
            )
        }

        item {
            VideoProcessingStatus(
                processingState = uiState.videoProcessingState,
                progress = uiState.videoProgress,
                processedCount = uiState.videoProcessedCount
            )
        }

        item {
            VideoControlButtons(
                processingState = uiState.videoProcessingState,
                hasVideo = uiState.videoUri != null,
                onStartProcessing = { viewModel.startVideoProcessing() },
                onCancelProcessing = { viewModel.cancelVideoProcessing() },
                onSaveVideo = { viewModel.saveVideoResult() }
            )
        }

        if (uiState.videoProcessedFrames.isNotEmpty()) {
            item {
                Text(
                    "处理结果预览 (${uiState.videoProcessedFrames.size} 帧)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(
                uiState.videoProcessedFrames.takeLast(20),
                key = { it.frameIndex }
            ) { frameResult ->
                VideoFramePreviewCard(frameResult)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun VideoSourceCard(
    videoUri: Uri?,
    videoInfo: com.Enco.facefound.video.VideoProcessor.VideoInfo?,
    onSelectVideo: () -> Unit
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
                "视频源",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            if (videoUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "选择视频文件开始识别",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSelectVideo,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("选择视频")
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            videoUri.lastPathSegment ?: "视频文件",
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (videoInfo != null && videoInfo.durationMs > 0) {
                            val seconds = videoInfo.durationMs / 1000
                            Text(
                                "${videoInfo.width}×${videoInfo.height} · ${seconds / 60}:${String.format("%02d", seconds % 60)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSelectVideo,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("重新选择")
                }
            }
        }
    }
}

@Composable
fun VideoSettingsCard(
    threshold: Float,
    onThresholdChange: (Float) -> Unit,
    sampleRate: Int,
    onSampleRateChange: (Int) -> Unit
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

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("相似度阈值", style = MaterialTheme.typography.bodyMedium)
                Text(
                    String.format("%.2f", threshold),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
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
                Text("采样间隔", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "每 $sampleRate 帧",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = sampleRate.toFloat(),
                onValueChange = { onSampleRateChange(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun VideoProcessingStatus(
    processingState: FaceRecognitionViewModel.VideoProcessingState,
    progress: Float,
    processedCount: Int
) {
    AnimatedVisibility(
        visible = processingState !is FaceRecognitionViewModel.VideoProcessingState.Idle,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (processingState) {
                    is FaceRecognitionViewModel.VideoProcessingState.Completed ->
                        MaterialTheme.colorScheme.primaryContainer
                    is FaceRecognitionViewModel.VideoProcessingState.Error ->
                        MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (processingState) {
                        is FaceRecognitionViewModel.VideoProcessingState.Processing -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                "正在处理... ${processedCount} 帧已完成",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is FaceRecognitionViewModel.VideoProcessingState.Completed -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "处理完成! 共 ${processedCount} 帧",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is FaceRecognitionViewModel.VideoProcessingState.Error -> {
                            Text(
                                "处理出错: ${processingState.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is FaceRecognitionViewModel.VideoProcessingState.Encoding -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                processingState.message ?: "正在编码视频...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        else -> {}
                    }
                }

                if (processingState is FaceRecognitionViewModel.VideoProcessingState.Processing ||
                    processingState is FaceRecognitionViewModel.VideoProcessingState.Encoding) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun VideoControlButtons(
    processingState: FaceRecognitionViewModel.VideoProcessingState,
    hasVideo: Boolean,
    onStartProcessing: () -> Unit,
    onCancelProcessing: () -> Unit,
    onSaveVideo: () -> Unit
) {
    val isIdle = processingState is FaceRecognitionViewModel.VideoProcessingState.Idle
    val isProcessing = processingState is FaceRecognitionViewModel.VideoProcessingState.Processing
    val isCompleted = processingState is FaceRecognitionViewModel.VideoProcessingState.Completed

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isIdle) {
            Button(
                onClick = onStartProcessing,
                enabled = hasVideo,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("开始识别")
            }
        } else if (isProcessing) {
            Button(
                onClick = onCancelProcessing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("取消")
            }
        } else if (isCompleted) {
            Button(
                onClick = onStartProcessing,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("重新识别")
            }
            Button(
                onClick = onSaveVideo,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("保存视频")
            }
        }
    }
}

@Composable
fun VideoFramePreviewCard(
    frame: com.Enco.facefound.ui.viewmodel.FaceRecognitionViewModel.VideoFrameResult
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "帧 #${frame.frameIndex}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${frame.detections.size} 张人脸",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (frame.names.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "识别: ${frame.names.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
