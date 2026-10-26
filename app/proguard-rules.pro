# ───────────────────────────────────────────────────────────────────────
# Noiddroid ProGuard rules
# ───────────────────────────────────────────────────────────────────────

# ── General Android ─────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod, SourceFile, LineNumberTable
# (Removed legacy `android.support.**` rules — noiddroid is 100% AndroidX.)

# ── Hilt / Dagger ──────────────────────────────────────────────────────
-keep,allowobfuscation,allowshrinking class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.HiltAndroidApp class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.lifecycle.HiltViewModel class *
-keepclassmembers class * { @javax.inject.Inject <init>(...); }
-keepnames class * { @dagger.hilt.android.HiltAndroidApp *; }
-keep class kotlin.Metadata { *; }

# ── Kotlinx Serialization ──────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep @kotlinx.serialization.Serializable class * {
    *;
}

# ── Retrofit / OkHttp ──────────────────────────────────────────────────
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Compose ────────────────────────────────────────────────────────────
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ── JNI bridge ──────────────────────────────────────────────────────────
-keep class org.secretarrow.noiddroid.mine.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── Sentry (mixed) ─────────────────────────────────────────────────────
-dontwarn io.sentry.**
-keep class io.sentry.** { *; }
-keep class org.secretarrow.noiddroid.BuildConfig { *; }
