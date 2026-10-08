# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room Database entities, DAOs, and TypeConverters
-keep class com.ilseon.AppDatabase { *; }
-keep class com.ilseon.AppDatabase$Converters { *; }
-keep class com.ilseon.data.** { *; }
-keep interface com.ilseon.data.**Dao { *; }

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
}
-keepclassmembers @kotlinx.serialization.Serializable class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Glance & App Widgets
-keep class com.ilseon.widget.** { *; }

# Dagger / Hilt entry points
-keep interface com.ilseon.**EntryPoint { *; }
