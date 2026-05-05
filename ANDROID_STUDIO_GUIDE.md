# Android Studio 项目导入指南

## 项目结构

```
android_project/
├── app/
│   ├── build.gradle.kts          # 应用级 Gradle 配置
│   ├── proguard-rules.pro        # ProGuard 混淆规则
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── assets/
│           │   ├── det_10g.onnx  # 人脸检测模型 (~16MB)
│           │   └── w600k_r50.onnx# 人脸识别模型 (~166MB)
│           ├── java/com/Enco/facefound/
│           │   ├── MainActivity.kt
│           │   ├── FaceRecognitionApp.kt
│           │   ├── ml/
│           │   │   └── OnnxFaceRecognition.kt
│           │   ├── ui/
│           │   │   ├── screens/
│           │   │   │   ├── MainScreen.kt
│           │   │   │   └── VideoScreen.kt
│           │   │   ├── viewmodel/
│           │   │   │   └── FaceRecognitionViewModel.kt
│           │   │   └── theme/
│           │   │       ├── Color.kt
│           │   │       ├── Theme.kt
│           │   │       └── Type.kt
│           │   ├── util/
│           │   │   ├── NpzParser.kt
│           │   │   └── TemplateRepository.kt
│           │   └── video/
│           │       └── VideoProcessor.kt
│           └── res/
│               ├── values/
│               │   ├── strings.xml
│               │   ├── colors.xml
│               │   └── themes.xml
│               └── xml/
│                   ├── file_paths.xml
│                   ├── data_extraction_rules.xml
│                   └── backup_rules.xml
├── build.gradle.kts              # 项目级 Gradle 配置
├── settings.gradle.kts           # 项目设置
└── gradle/
    └── wrapper/
        └── gradle-wrapper.properties
```

---

## 导入步骤

### 1. 打开 Android Studio

启动 Android Studio（推荐版本：Giraffe 2023.1.1 或更新）

### 2. 导入项目

```
File -> Open -> 选择 android_project 文件夹
```

### 3. 等待 Gradle 同步

Android Studio 会自动下载依赖并同步项目。首次同步可能需要几分钟。

### 4. 配置 SDK

确保已安装：

- Android SDK 34 (Android 14)
- Android SDK Build-Tools 34
- Android Emulator（可选）

```
File -> Settings -> Appearance & Behavior -> System Settings -> Android SDK
```

### 5. 运行项目

点击 Run 按钮，或按 `Shift + F10`

---

## 构建 APK

### Debug 版本

```
Build -> Build Bundle(s) / APK(s) -> Build APK(s)
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### Release 版本

```
Build -> Generate Signed Bundle / APK -> APK
```

需要配置签名密钥。

---

## 常见问题

### Gradle 同步失败

**解决：**

```
File -> Invalidate Caches / Restart -> Invalidate and Restart
```

### 依赖下载慢

**解决：** 在 `settings.gradle.kts` 中添加阿里云镜像：

```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}
```

### 编译错误

**检查：**

1. JDK 版本是否为 17
2. Kotlin 插件是否最新
3. Compose 编译器版本是否匹配

### 模型文件缺失

**解决：** 将 `det_10g.onnx` 和 `w600k_r50.onnx` 复制到 `app/src/main/assets/` 目录

---

## 功能说明

### 已实现功能

- [x] Material Design 3 界面
- [x] 图片选择和预览
- [x] ONNX Runtime 人脸检测与识别
- [x] NPZ 模板导入与管理
- [x] 识别历史记录
- [x] 视频人脸识别
- [x] 结果图片/视频保存到相册
- [x] 深色/浅色主题切换
- [x] 相似度阈值调节

### 开发计划

- [ ] CameraX 实时相机识别
- [ ] 多语言国际化
- [ ] 批量图片处理

---

## 下一步开发

1. **相机功能**
   - 使用 CameraX 库
   - 实时预览和识别
   - 帧率优化

2. **性能优化**
   - NNAPI 加速
   - 模型量化
   - 多线程推理

3. **功能扩展**
   - 人脸属性检测（年龄、性别）
   - 多人脸跟踪
   - 云端模板同步
