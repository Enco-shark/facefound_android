# 🤖 Android Studio 项目导入指南

## 📁 项目结构

```
android_project/
├── app/
│   ├── build.gradle.kts          # 应用级 Gradle 配置
│   ├── proguard-rules.pro        # ProGuard 混淆规则
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/Enco/facefound/
│           │   ├── MainActivity.kt
│           │   ├── FaceRecognitionApp.kt
│           │   └── ui/
│           │       ├── screens/
│           │       │   └── MainScreen.kt
│           │       ├── viewmodel/
│           │       │   └── FaceRecognitionViewModel.kt
│           │       └── theme/
│           │           ├── Color.kt
│           │           ├── Theme.kt
│           │           └── Type.kt
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

## 🚀 导入步骤

### 1. 打开 Android Studio

启动 Android Studio（建议版本：Giraffe 或更新）

### 2. 导入项目

```
File → Open → 选择 android_project 文件夹
```

### 3. 等待 Gradle 同步

Android Studio 会自动下载依赖并同步项目。这可能需要几分钟。

### 4. 配置 SDK

确保已安装：
- Android SDK 34
- Android SDK Build-Tools 34
- Android Emulator（可选）

```
File → Settings → Appearance & Behavior → System Settings → Android SDK
```

### 5. 运行项目

点击 ▶️ 运行按钮，或按 `Shift + F10`

---

## 📱 构建 APK

### Debug 版本

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### Release 版本

```
Build → Generate Signed Bundle / APK → APK
```

需要配置签名密钥。

---

## 🔧 常见问题

### Gradle 同步失败

**解决：**
```
File → Invalidate Caches / Restart → Invalidate and Restart
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

---

## 📝 功能说明

### 当前实现

- ✅ Material Design 3 界面
- ✅ 图片选择和预览
- ✅ ML Kit 人脸检测
- ✅ 阈值调节
- ✅ 日志显示

### 待实现

- 🔄 模板加载（npz 文件解析）
- 🔄 人脸特征比对
- 🔄 结果图片标注
- 🔄 相机实时识别

---

## 🎯 下一步开发

1. **集成 InsightFace**
   - 使用 TensorFlow Lite 转换模型
   - 或调用 Python 后端 API

2. **添加相机功能**
   - 使用 CameraX 库
   - 实时预览和识别

3. **优化性能**
   - 图片压缩
   - 异步处理
   - 缓存机制

---

**Happy Coding! 🚀**
