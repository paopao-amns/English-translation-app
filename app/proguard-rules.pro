# English Learner ProGuard Rules

# --- kotlinx.serialization (official rules) ---
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

# --- App model classes (keep all data classes) ---
-keep class com.paopao.englearn.data.local.entity.** { *; }
-keep class com.paopao.englearn.data.remote.** { *; }
-keep class com.paopao.englearn.domain.model.** { *; }

# --- Retrofit ---
-keep,allowobfuscation interface com.paopao.englearn.data.remote.ApiService
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# --- Okio (used by OkHttp) ---
-dontwarn org.codehaus.mojo.animal_sniffer.**

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- ML Kit ---
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }

# --- CameraX ---
-dontwarn androidx.camera.**
-keep class androidx.camera.** { *; }

# --- Coil ---
-dontwarn coil.**
-keep class coil.** { *; }

# --- DataStore ---
-dontwarn androidx.datastore.**
-keep class androidx.datastore.** { *; }

# --- Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# --- General Android ---
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
