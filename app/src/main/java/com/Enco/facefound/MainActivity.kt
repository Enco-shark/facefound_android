package com.Enco.facefound // 声明包名为应用根包

import android.Manifest // 导入Android权限常量
import android.content.pm.PackageManager // 导入包管理器，用于权限检查
import android.os.Build // 导入Build类，用于版本判断
import android.os.Bundle // 导入Bundle类，用于Activity状态保存
import androidx.activity.ComponentActivity // 导入ComponentActivity基类
import androidx.activity.compose.setContent // 导入setContent扩展函数，设置Compose内容
import androidx.activity.result.contract.ActivityResultContracts // 导入权限请求合约
import androidx.compose.foundation.layout.fillMaxSize // 导入fillMaxSize修饰符
import androidx.compose.material3.MaterialTheme // 导入Material3主题
import androidx.compose.material3.Surface // 导入Surface容器组件
import androidx.compose.runtime.collectAsState // 导入StateFlow的collectAsState
import androidx.compose.runtime.getValue // 导入by委托取值
import androidx.compose.ui.Modifier // 导入Compose修饰符
import androidx.lifecycle.ViewModelProvider // 导入ViewModel提供者
import androidx.core.content.ContextCompat // 导入兼容版权限检查工具
import com.Enco.facefound.ui.theme.FaceRecognitionTheme // 导入应用主题
import com.Enco.facefound.ui.screens.MainScreen // 导入主屏幕Composable
import com.Enco.facefound.ui.viewmodel.FaceRecognitionViewModel // 导入ViewModel

class MainActivity : ComponentActivity() { // 主Activity，继承ComponentActivity以支持Compose

    private lateinit var viewModel: FaceRecognitionViewModel // 延迟初始化的ViewModel引用
    private var isInitialized = false // 标记ViewModel是否已初始化，防止重复初始化

    private val permissionLauncher = registerForActivityResult( // 注册权限请求回调
        ActivityResultContracts.RequestMultiplePermissions() // 使用多权限请求合约
    ) { permissions -> // 权限结果回调
        try { // 异常保护，防止回调NPE
            val allGranted = if (permissions.isNotEmpty()) permissions.values.all { it } else false // 检查是否所有权限都已授予
            if (allGranted) { // 所有权限已授予
                initializeViewModelIfNeeded() // 初始化ViewModel
            } else { // 部分或全部权限被拒绝
                initializeViewModelIfNeeded() // 仍然初始化（功能可能受限）
            }
        } catch (_: Exception) { // 权限回调异常
            initializeViewModelIfNeeded() // 异常时仍尝试初始化
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) { // Activity创建生命周期回调
        super.onCreate(savedInstanceState) // 调用父类onCreate

        viewModel = ViewModelProvider(this)[FaceRecognitionViewModel::class.java] // 通过ViewModelProvider获取或创建ViewModel

        requestStoragePermission() // 请求存储和相机权限

        setContent { // 设置Compose UI内容
            val uiState by viewModel.uiState.collectAsState() // 收集ViewModel的UI状态Flow
            FaceRecognitionTheme(darkTheme = uiState.isDarkTheme) { // 应用主题，传入深色模式状态
                Surface( // Surface容器，提供背景色
                    modifier = Modifier.fillMaxSize(), // 填满整个屏幕
                    color = MaterialTheme.colorScheme.background // 使用主题背景色
                ) {
                    MainScreen(viewModel = viewModel) // 渲染主屏幕，传入ViewModel
                }
            }
        }
    }

    private fun initializeViewModelIfNeeded() { // 条件初始化ViewModel
        if (!isInitialized) { // 尚未初始化
            isInitialized = true // 标记为已初始化
            viewModel.initialize() // 调用ViewModel的初始化方法
        }
    }

    private fun requestStoragePermission() { // 请求存储和相机权限
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            Manifest.permission.READ_MEDIA_IMAGES // 使用新的媒体图片权限
        } else { // Android 12及以下
            Manifest.permission.READ_EXTERNAL_STORAGE // 使用传统外部存储权限
        }

        if (ContextCompat.checkSelfPermission(this, storagePermission) == PackageManager.PERMISSION_GRANTED && // 存储权限已授予
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { // 相机权限已授予
            // 权限已授予，直接初始化
            initializeViewModelIfNeeded() // 两个权限都有，直接初始化
        } else { // 需要请求权限
            // 请求权限
            permissionLauncher.launch(arrayOf(storagePermission, Manifest.permission.CAMERA)) // 启动权限请求对话框
        }
    }

    override fun onDestroy() { // Activity销毁生命周期回调
        super.onDestroy() // 调用父类onDestroy
        // ViewModel 会自动清理
    }
}
