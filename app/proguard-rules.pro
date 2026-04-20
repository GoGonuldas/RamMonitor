
# --- General ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlin / Coroutines ---
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# --- AndroidX / Lifecycle ---
-dontwarn androidx.**
-keep class androidx.lifecycle.** { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# --- MPAndroidChart ---
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# --- App models used with reflection (ViewBinding / Room entities) ---
-keep class com.rammonitor.data.** { *; }

# --- Services / Receivers / Activities (kept by manifest, extra safety) ---
-keep class com.rammonitor.service.** { *; }
-keep class com.rammonitor.ui.** { *; }

