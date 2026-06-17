-keep class com.javis.ai.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.* class * { *; }
-keep class * implements androidx.room.* { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.* class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keep class * extends android.service.notification.NotificationListenerService { *; }
-keep class * extends android.accessibilityservice.AccessibilityService { *; }
-keep class * extends android.service.quicksettings.TileService { *; }
-keep class * extends android.app.Service { *; }
