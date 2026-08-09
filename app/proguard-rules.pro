# Local AI Painter v3.0 ProGuard Rules

# ========== 基础 ==========
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,Exceptions,InnerClasses,EnclosingMethod

# ========== JNI / Native ==========
# 保留所有 JNI 方法签名
-keepclasseswithmembernames class * {
    native <methods>;
}

# EngineFactory 和所有引擎类
-keep class com.localaipainter.engine.** { *; }
-keep class com.localaipainter.core.** { *; }

# ========== Compose ==========
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# ========== MNN ==========
-dontwarn com.alibaba.mnn.**
-keep class com.alibaba.mnn.** { *; }

# ========== NCNN ==========
-dontwarn com.tencent.ncnn.**
-keep class com.tencent.ncnn.** { *; }

# ========== ONNX Runtime ==========
-dontwarn ai.onnxruntime.**
-keep class ai.onnxruntime.** { *; }

# ========== Coroutines ==========
-dontwarn kotlinx.coroutines.**
-keepclassmembernames class kotlinx.coroutines.** { *; }

# ========== Room ==========
-keep class com.localaipainter.data.db.** { *; }
-keep @androidx.room.Entity class *

# ========== DataStore ==========
-keep class com.localaipainter.data.preferences.** { *; }

# ========== Model Serialization ==========
# 保留模型元数据类
-keep class com.localaipainter.models.** { *; }

# ========== 移除日志 ==========
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
