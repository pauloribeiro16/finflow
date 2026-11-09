-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn net.zetetic.database.**

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
