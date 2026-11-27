# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep interface retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# OkHttp
-keep class okhttp3.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.squareup.moshi.**

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Coil
-keep class coil3.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn coil3.**

# ExoPlayer
-keep class com.google.android.exoplayer2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.android.exoplayer2.**

# Media3
-keep class androidx.media3.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn androidx.media3.**

# Hilt
-keep class dagger.hilt.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Room
-keep class androidx.room.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# DataStore
-keep class androidx.datastore.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Paging
-keep class androidx.paging.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# WorkManager
-keep class androidx.work.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Navigation
-keep class androidx.navigation.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Compose
-keep class androidx.compose.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Material3
-keep class androidx.compose.material3.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Application classes
-keep class com.rapzone.app.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Serialization
-keep class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# WebView
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Annotations
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod