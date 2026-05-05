package com.Enco.facefound

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.ContextCompat
import com.Enco.facefound.ui.theme.FaceRecognitionTheme
import com.Enco.facefound.ui.screens.MainScreen
import com.Enco.facefound.ui.viewmodel.FaceRecognitionViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: FaceRecognitionViewModel
    private var isInitialized = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        try {
            val allGranted = if (permissions.isNotEmpty()) permissions.values.all { it } else false
            if (allGranted) {
                initializeViewModelIfNeeded()
            } else {
                initializeViewModelIfNeeded()
            }
        } catch (_: Exception) {
            initializeViewModelIfNeeded()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[FaceRecognitionViewModel::class.java]

        requestStoragePermission()
        
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            FaceRecognitionTheme(darkTheme = uiState.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun initializeViewModelIfNeeded() {
        if (!isInitialized) {
            isInitialized = true
            viewModel.initialize()
        }
    }

    private fun requestStoragePermission() {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, storagePermission) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // 权限已授予，直接初始化
            initializeViewModelIfNeeded()
        } else {
            // 请求权限
            permissionLauncher.launch(arrayOf(storagePermission, Manifest.permission.CAMERA))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel 会自动清理
    }
}