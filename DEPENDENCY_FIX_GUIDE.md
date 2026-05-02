# 依赖修复指南

## 问题：Android Studio 报错缺少 JAR

### 原因
Gradle 需要从 Maven 仓库下载依赖，但可能由于以下原因失败：
1. 网络问题（国内访问 Maven Central 慢）
2. 仓库配置问题
3. 某些依赖版本已下架

### 解决方案

#### 方案 1：配置国内镜像（推荐）

在 `settings.gradle.kts` 或项目根目录 `build.gradle` 中添加国内镜像：

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
    }
}
```

#### 方案 2：手动下载 JAR

1. 访问 https://mvnrepository.com/
2. 搜索需要的依赖（如 `onnxruntime-android`）
3. 下载对应版本的 `.aar` 或 `.jar` 文件
4. 放入 `app/libs/` 目录

#### 方案 3：使用离线模式

如果已经下载过依赖，可以启用 Gradle 离线模式：
1. Android Studio → File → Settings → Build, Execution, Deployment → Build Tools → Gradle
2. 勾选 "Offline work"

### 关键依赖下载地址

| 依赖 | 下载地址 |
|------|----------|
| ONNX Runtime 1.17.0 | https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.17.0/ |
| Compose BOM 2024.02.00 | https://repo1.maven.org/maven2/androidx/compose/compose-bom/2024.02.00/ |
| Coil 2.5.0 | https://repo1.maven.org/maven2/io/coil-kt/coil-compose/2.5.0/ |

### AAR 文件使用方法

如果下载的是 `.aar` 文件：

1. 将 `.aar` 文件放入 `app/libs/`
2. 在 `app/build.gradle.kts` 中添加：

```kotlin
dependencies {
    implementation(files("libs/onnxruntime-android-1.17.0.aar"))
    // ... 其他依赖
}
```

### 验证依赖

在 Android Studio 终端中运行：

```bash
./gradlew app:dependencies --configuration implementation
```

查看所有已解析的依赖。
