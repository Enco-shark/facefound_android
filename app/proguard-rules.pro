# ProGuard / R8 rules for Face Recognition App
# ✅ P2-4 修复：补全 keep 规则，配合 isMinifyEnabled=true 使用

# === Compose ===
-keep class androidx.compose.** { *; }
-keep class androidx.compose.material3.** { *; }
# Compose 编译器生成的代码
-keep class androidx.compose.runtime.** { *; }

# === Kotlin metadata ===
# 保留 Kotlin 反射元数据，Compose/Kotlinx 序列化等依赖
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
# Kotlin 协程内部类
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# === ONNX Runtime ===
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }
# ONNX native 方法（JNI）
-keepclasseswithmembernames class * {
    native <methods>;
}

# === 项目数据类 ===
# 保留所有 data class，避免反射访问字段时被移除
-keep class com.Enco.facerecognition.ml.OnnxFaceRecognition$FaceDetection { *; }
-keep class com.Enco.facerecognition.ml.OnnxFaceRecognition$RecognitionResult { *; }
-keep class com.Enco.facerecognition.video.VideoProcessor$VideoFrameResult { *; }
-keep class com.Enco.facerecognition.video.VideoProcessor$VideoInfo { *; }
-keep class com.Enco.facerecognition.ui.viewmodel.FaceRecognitionViewModel$UiState { *; }
-keep class com.Enco.facerecognition.ui.viewmodel.FaceRecognitionViewModel$VideoFrameResult { *; }

# === AndroidX 通用 ===
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }

# === 通用属性 ===
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# === Coil 图片加载 ===
-keep class coil.** { *; }
