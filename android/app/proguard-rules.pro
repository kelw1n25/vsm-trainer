# kotlinx.serialization: сохранить сгенерированные сериализаторы DTO
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class ru.vsm.trainer.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class ru.vsm.trainer.data.remote.dto.** { *; }
# Retrofit: интерфейсы API читаются через рефлексию
-keep,allowobfuscation interface ru.vsm.trainer.data.remote.TrainerApi
-keepattributes Signature, Exceptions
