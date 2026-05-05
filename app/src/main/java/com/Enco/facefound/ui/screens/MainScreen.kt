package com.Enco.facefound.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.Enco.facefound.BuildConfig
import com.Enco.facefound.ui.viewmodel.FaceRecognitionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- 主屏幕 ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FaceRecognitionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 图片选择器
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setInputImage(it) }
    }

    // 模板选择器
    val templatePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setTemplate(it) }
    }

    // 使用 remember 缓存回调，避免不必要的重组
    val onDrawerClose = remember<() -> Unit> {
        { scope.launch { drawerState.close() } }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                viewModel = viewModel,
                onItemClick = onDrawerClose
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("FaceFound") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(Icons.Default.Palette, contentDescription = "切换主题")
                        }
                    }
                )
            }
        ) { paddingValues ->
            when (uiState.currentScreen) {
                FaceRecognitionViewModel.Screen.Main -> {
                    MainContent(
                        viewModel = viewModel,
                        imagePicker = { imagePicker.launch("image/*") },
                        templatePicker = { templatePicker.launch("*/*") },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                FaceRecognitionViewModel.Screen.Templates -> {
                    TemplatesScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                FaceRecognitionViewModel.Screen.History -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                FaceRecognitionViewModel.Screen.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                FaceRecognitionViewModel.Screen.Camera -> {
                    Text("相机功能开发中...", modifier = Modifier.padding(paddingValues))
                }
                FaceRecognitionViewModel.Screen.Video -> {
                    VideoScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

// --- 侧滑菜单 ---

@Composable
fun DrawerContent(
    viewModel: FaceRecognitionViewModel,
    onItemClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val screens = listOf(
        FaceRecognitionViewModel.Screen.Main to ("主页" to Icons.Default.Image),
        FaceRecognitionViewModel.Screen.Video to ("视频识别" to Icons.Default.Videocam),
        FaceRecognitionViewModel.Screen.Templates to ("模板管理" to Icons.Default.Folder),
        FaceRecognitionViewModel.Screen.History to ("识别历史" to Icons.Default.History),
        FaceRecognitionViewModel.Screen.Settings to ("设置" to Icons.Default.Settings)
    )

    Column(
        Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(24.dp)
                .padding(top = 40.dp)
        ) {
            Column {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "FaceFound",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "离线人脸识别",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(Modifier.height(8.dp))

        screens.forEachIndexed { index, (screen, pair) ->
            val (title, icon) = pair
            DrawerMenuItem(
                title = title,
                icon = icon,
                screen = screen,
                currentScreen = uiState.currentScreen,
                onClick = {
                    viewModel.navigateTo(screen)
                    onItemClick()
                }
            )
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Text(
            "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun DrawerMenuItem(
    title: String,
    icon: ImageVector,
    screen: FaceRecognitionViewModel.Screen,
    currentScreen: FaceRecognitionViewModel.Screen,
    onClick: () -> Unit
) {
    val isSelected = screen == currentScreen
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else Color.Transparent,
        label = "drawerBg"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface,
        label = "drawerContent"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// --- 主页内容 ---

@Composable
fun MainContent(
    viewModel: FaceRecognitionViewModel,
    imagePicker: () -> Unit,
    templatePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 状态卡片
        item {
            StatusCard(uiState.statusMessage, uiState.isReady)
        }

        // 图片预览
        item {
            ImagePreviewCard(
                imageUri = uiState.inputImageUri,
                resultBitmap = uiState.resultBitmap,
                onSelectImage = { imagePicker() },
                onReselectImage = { imagePicker() }
            )
        }

        // 控制按钮
        item {
            ControlPanel(
                templateName = uiState.templateName,
                onLoadTemplate = { templatePicker() },
                onRecognize = { viewModel.startRecognition() },
                onSaveImage = { viewModel.saveResultImage() },
                isProcessing = uiState.isProcessing,
                canSave = uiState.resultBitmap != null
            )
        }

        // 阈值滑块
        item {
            ThresholdSlider(
                threshold = uiState.threshold,
                onThresholdChange = { viewModel.updateThreshold(it) }
            )
        }

        // 日志卡片
        item {
            LogCard(logs = uiState.logs)
        }
    }
}

// --- 模板管理屏幕 ---

@Composable
fun TemplatesScreen(
    viewModel: FaceRecognitionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRenameDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    val templatePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setTemplate(it) }
    }

    LazyColumn(modifier.padding(16.dp)) {
        item {
            Text(
                "模板管理",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { templatePicker.launch("*/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("导入 NPZ")
                }
                if (uiState.templateList.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("清空全部")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        if (uiState.templateList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text(
                            "暂无模板",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "请导入 NPZ 模板文件，导入后将自动持久化保存",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${uiState.templateList.size} 个模板已持久化 · 下次启动无需重新导入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(uiState.templateList, key = { it.name }) { template ->
                TemplateItemCard(
                    template = template,
                    onDelete = { showDeleteDialog = it },
                    onRename = { showRenameDialog = it }
                )
            }
        }
    }

    showRenameDialog?.let { name ->
        var newName by remember { mutableStateOf(name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("重命名模板") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("新名称") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotEmpty()) {
                        viewModel.renameTemplate(name, newName)
                        showRenameDialog = null
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    showDeleteDialog?.let { name ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除模板") },
            text = { Text("确定要删除模板 \"$name\" 吗？") },
            confirmButton = {
                Button(onClick = {
                    viewModel.removeTemplate(name)
                    showDeleteDialog = null
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空全部模板") },
            text = { Text("确定要清空所有 ${uiState.templateList.size} 个人脸模板吗？\n\n注意：模板数据将永久删除，需要重新导入 NPZ 文件。") },
            confirmButton = {
                Button(onClick = {
                    uiState.templateList.forEach { viewModel.removeTemplate(it.name) }
                    showClearDialog = false
                }) {
                    Text("全部清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun TemplateItemCard(
    template: FaceRecognitionViewModel.TemplateItem,
    onDelete: (String) -> Unit,
    onRename: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    template.name.firstOrNull()?.toString() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                template.name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { onRename(template.name) }) {
                Icon(Icons.Default.Edit, contentDescription = "重命名")
            }
            IconButton(onClick = { onDelete(template.name) }) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    }
}

// --- 识别历史屏幕 ---

@Composable
fun HistoryScreen(
    viewModel: FaceRecognitionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier.padding(16.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "识别历史",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (uiState.recognitionHistory.isNotEmpty()) {
                    Button(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空")
                        Spacer(Modifier.width(4.dp))
                        Text("清空")
                    }
                }
            }
        }

        if (uiState.recognitionHistory.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "暂无识别历史",
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(uiState.recognitionHistory, key = { it.id }) { item ->
                HistoryItemCard(item)
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空历史") },
            text = { Text("确定要清空所有识别历史吗？") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearHistory()
                    showClearDialog = false
                }) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun HistoryItemCard(item: FaceRecognitionViewModel.RecognitionHistoryItem) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                dateFormat.format(item.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "识别结果: ${item.recognizedNames.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "耗时: ${item.processingTimeMs}ms",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// --- 设置屏幕 ---

@Composable
fun SettingsScreen(
    viewModel: FaceRecognitionViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(modifier.padding(16.dp)) {
        item {
            Text(
                "设置",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            SettingGroup(title = "外观") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("深色主题")
                    Switch(
                        checked = uiState.isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme() }
                    )
                }
            }
        }

        item {
            SettingGroup(title = "识别设置") {
                Column(Modifier.padding(vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("相似度阈值", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            String.format("%.2f", uiState.threshold),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = uiState.threshold,
                        onValueChange = { viewModel.updateThreshold(it) },
                        valueRange = 0.0f..1.0f,
                        steps = 99,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("宽松", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("严格", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("自动降采样大图")
                    Switch(
                        checked = uiState.imageDownsample,
                        onCheckedChange = { viewModel.setImageDownsample(it) }
                    )
                }
            }
        }

        item {
            SettingGroup(title = "关于") {
                Text(
                    "FaceFound v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

// --- 复用组件 ---

@Composable
fun StatusCard(statusMessage: String, isReady: Boolean) {
    val containerColor = if (isReady)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isReady) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (isReady) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                statusMessage,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ImagePreviewCard(
    imageUri: Uri?,
    resultBitmap: android.graphics.Bitmap?,
    onSelectImage: () -> Unit,
    onReselectImage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                resultBitmap != null -> {
                    Image(
                        bitmap = resultBitmap.asImageBitmap(),
                        contentDescription = "识别结果",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                imageUri != null -> {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "预览图片",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    IconButton(onClick = onSelectImage, modifier = Modifier.fillMaxSize()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "选择图片",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "点击选择图片",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (imageUri != null || resultBitmap != null) {
                Button(
                    onClick = onReselectImage,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("重新选择", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ControlPanel(
    templateName: String?,
    onLoadTemplate: () -> Unit,
    onRecognize: () -> Unit,
    onSaveImage: () -> Unit,
    isProcessing: Boolean,
    canSave: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            OutlinedButton(
                onClick = onLoadTemplate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    templateName ?: "加载模板",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRecognize,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (isProcessing) "识别中..." else "开始识别")
                }

                Button(
                    onClick = onSaveImage,
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("保存")
                }
            }
        }
    }
}

@Composable
fun ThresholdSlider(threshold: Float, onThresholdChange: (Float) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "相似度阈值: ${String.format("%.2f", threshold)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Slider(
                value = threshold,
                onValueChange = onThresholdChange,
                valueRange = 0.0f..1.0f,
                steps = 99
            )
        }
    }
}

@Composable
fun LogCard(logs: List<String>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "日志",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.inverseOnSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                LazyColumn(
                    Modifier.padding(12.dp)
                ) {
                    items(logs.reversed()) { log ->
                        Text(
                            log,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
