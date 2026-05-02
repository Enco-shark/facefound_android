此目录用于放置本地 JAR 文件。

如果 Gradle 同步时某些依赖无法从 Maven 仓库下载，可以手动下载 JAR 文件放到此目录。

可能需要手动下载的 JAR：
1. onnxruntime-android-1.17.0.aar (从 Maven Central 下载)
   下载地址: https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.17.0/
   
2. 其他依赖如果下载失败，可以从 https://mvnrepository.com/ 搜索并下载对应版本的 JAR/AAR

注意：.aar 文件需要解压后将 classes.jar 放入此目录，或直接在 build.gradle 中引用 .aar 文件。
