# English Learner ProGuard Rules

# Keep serializable classes used by kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities
-keep class com.paopao.englearn.data.local.entity.** { *; }

# Keep Retrofit interfaces
-keep,allowobfuscation interface com.paopao.englearn.data.remote.ApiService

# Keep domain models (used by kotlinx.serialization)
-keep class com.paopao.englearn.data.remote.** { *; }
-keep class com.paopao.englearn.domain.model.** { *; }
