# ═══════════════════════════════════════════
# WaterSMS — ProGuard / R8 Optimization Rules
# ═══════════════════════════════════════════

# ─── General Android ────────────────────────
-keepattributes *Annotation*, Signature, Exception, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

-keepclassmembers class * extends android.app.Activity { public *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * implements android.os.Parcelable { *; }

# Keep the application class
-keep class jv.watersms.enterprises.WaterSmsApp { *; }

# ─── Kotlin ─────────────────────────────────
-keepclassmembers class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.**

# ─── Jetpack Compose ────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ─── Room ───────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ─── Hilt / Dagger ──────────────────────────
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class dagger.hilt.** { *; }
-keepclassmembers class * {
    @dagger.hilt.android.internal.lifecycle.HiltViewModelMap <fields>;
}
-dontwarn dagger.**

# ─── Moshi ──────────────────────────────────
-keepclassmembers @com.squareup.moshi.JsonClass class * { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class ** {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep class **JsonAdapter { *; }
-dontwarn com.squareup.moshi.**

# ─── Retrofit ───────────────────────────────
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ─── OkHttp ─────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ─── Firebase ───────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ─── Gemini API Data Classes ────────────────
-keep class jv.watersms.enterprises.data.** { *; }
-keepclassmembers class jv.watersms.enterprises.data.** { *; }

# ─── Campaign & Recipient Entities ──────────
-keep class jv.watersms.enterprises.data.Campaign { *; }
-keep class jv.watersms.enterprises.data.Recipient { *; }

# ─── libphonenumber ─────────────────────────
-keep class com.google.i18n.phonenumbers.** { *; }
-dontwarn com.google.i18n.phonenumbers.**

# ─── Kotlinx Coroutines ─────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ─── Navigation Compose ─────────────────────
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ─── Lifecycle ──────────────────────────────
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ─── Remove Logging in Release ──────────────
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
