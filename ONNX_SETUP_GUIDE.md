# ONNX Runtime Mobile 设置指南

## 项目结构

```
android_project/
├── app/
│   ├── build.gradle.kts          # 已添加 ONNX Runtime 依赖
│   └── src/main/
│       ├── java/com/Enco/facefound/
│       │   ├── MainActivity.kt   # 主 Activity
│       │   ├── ml/
│       │   │   └── OnnxFaceRecognition.kt  # ONNX 推理类
│       │   ├── ui/
│       │   │   ├── screens/
│       │   │   │   ├── MainScreen.kt
│       │   │   │   └── VideoScreen.kt
│       │   │   └── viewmodel/
│       │   │       └── FaceRecognitionViewModel.kt
│       │   ├── util/
│       │   │   ├── NpzParser.kt
│       │   │   └── TemplateRepository.kt
│       │   └── video/
│       │       └── VideoProcessor.kt
│       └── assets/               # 模型文件目录
│           ├── det_10g.onnx      #   人脸检测模型 (~16MB)
│           └── w600k_r50.onnx    #   人脸识别模型 (~166MB)
```

---

## 快速开始

### 1. 复制模型文件

将 `buffalo_l` 目录中的 ONNX 模型复制到 Android 项目的 assets：

```bash
# Windows
xcopy /Y "buffalo_l\det_10g.onnx" "android_project\app\src\main\assets\"
xcopy /Y "buffalo_l\w600k_r50.onnx" "android_project\app\src\main\assets\"

# Linux/Mac
cp buffalo_l/det_10g.onnx android_project/app/src/main/assets/
cp buffalo_l/w600k_r50.onnx android_project/app/src/main/assets/
```

### 2. 导入 Android Studio

```
File -> Open -> 选择 android_project 文件夹
```

### 3. 同步 Gradle

点击 "Sync Now" 或按 `Ctrl+Shift+O`

### 4. 运行项目

点击 Run 按钮

---

## 使用说明

### 模型加载

应用启动时会自动加载 ONNX 模型：

```kotlin
// MainActivity 中自动初始化
viewModel.initialize(this)
```

### 人脸检测

```kotlin
val detections = faceRecognizer.detectFaces(bitmap)
```

### 人脸识别

```kotlin
val result = faceRecognizer.recognizeFace(faceBitmap, templates, threshold)
```

### 视频识别

```kotlin
val processor = VideoProcessor(faceRecognizer)
processor.processVideoFrames(context, videoUri, templates, threshold, onProgress)
    .collect { frame -> /* 处理每帧结果 */ }
```

---

## 配置说明

### Gradle 依赖

```kotlin
// ONNX Runtime Android
debugImplementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")
```

### 支持的架构

| 架构 | 支持 |
|------|------|
| arm64-v8a | 支持 |
| armeabi-v7a | 支持 |
| x86_64 | 支持 |

### 模型要求

| 模型 | 输入尺寸 | 输出 |
|------|----------|------|
| det_10g.onnx | 640x640 | 检测框 + 置信度 + 5 关键点 |
| w600k_r50.onnx | 112x112 | 512 维特征向量 |

---

## 故障排除

### 模型加载失败

**检查：**

1. 模型文件是否在 `assets/` 目录
2. 文件名是否正确
3. 文件是否损坏

### 推理失败

**检查：**

1. 输入图片尺寸是否正确
2. 内存是否充足
3. 模型版本是否匹配

### 性能问题

**优化：**

1. 使用 NNAPI 加速（已自动启用）
2. 降低输入图片分辨率
3. 减少并发推理
4. 启用 largeHeap（已配置）

---

## 性能参考

| 设备 | 检测时间 | 识别时间 |
|------|----------|----------|
| 骁龙 8 Gen 3 | ~30ms | ~20ms |
| 骁龙 8 Gen 2 | ~50ms | ~30ms |
| 骁龙 7 Gen 1 | ~100ms | ~60ms |

---

## 相关链接

- [ONNX Runtime Android](https://onnxruntime.ai/docs/tutorials/mobile/)
- [InsightFace 模型](https://github.com/deepinsight/insightface)
- [Android NDK](https://developer.android.com/ndk)

---

> 模型文件较大（约 180MB），请确保手机有足够存储空间！
