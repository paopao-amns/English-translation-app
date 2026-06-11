# English Learner ProGuard Rules

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.paopao.englearn.**$$serializer { *; }
-keepclassmembers class com.paopao.englearn.** {
    *** Companion;
}
-keepclasseswithmembers class com.paopao.englearn.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- App model classes ---
-keep class com.paopao.englearn.data.local.entity.** { *; }
-keep class com.paopao.englearn.data.remote.** { *; }
-keep class com.paopao.englearn.domain.model.** { *; }

# --- Retrofit ---
-keep,allowobfuscation interface com.paopao.englearn.data.remote.ApiService
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- ML Kit ---
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# --- CameraX ---
-dontwarn androidx.camera.**
-keep class androidx.camera.** { *; }

# --- Coil ---
-dontwarn coil.**
-keep class coil.** { *; }

# --- DataStore ---
-dontwarn androidx.datastore.**
-keep class androidx.datastore.** { *; }
