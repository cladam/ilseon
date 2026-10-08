# Wear OS ProGuard Rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep data models used across DataLayer and tiles
-keep class com.ilseon.wear.tile.WearTaskData { *; }

# Keep complication services and tile services referenced in AndroidManifest
-keep class com.ilseon.wear.complication.** { *; }
-keep class com.ilseon.wear.tile.** { *; }
