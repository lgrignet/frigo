# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep class com.mystockmanager.app.data.local.entities.** { *; }
-keep class com.mystockmanager.app.data.local.dao.** { *; }

# --- Hilt ---
-keep class com.mystockmanager.app.** { *; }

# --- Kotlin Serialization ---
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# --- AdMob ---
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# --- ZXing ---
-keep class com.google.zxing.** { *; }

# --- Ktor / SLF4J (Silence missing classes) ---
-dontwarn org.slf4j.**
-dontwarn io.ktor.**

