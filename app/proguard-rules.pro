# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Appodeal SDK
-keep class com.appodeal.** { *; }
-dontwarn com.appodeal.**

# Keep data classes and entities
-keep class com.gastonlesbegueris.caretemplate.data.local.** { *; }
-keep class com.gastonlesbegueris.caretemplate.data.sync.** { *; }

# Keep UI classes that use reflection
-keep class com.gastonlesbegueris.caretemplate.ui.** { *; }

# Keep utility classes
-keep class com.gastonlesbegueris.caretemplate.util.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}