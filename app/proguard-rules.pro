# BeatPad Proguard rules
-keepattributes *Annotation*
-dontwarn javax.annotation.**

# Room Database
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class * extends androidx.room.RoomDatabase

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class com.beatpad.app.data.model.** { *; }
-keepclassmembers class com.beatpad.app.data.local.** { *; }
