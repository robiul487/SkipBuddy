# SkipBuddy R8 / ProGuard keep rules

# Preserve Android entry points (Activity, AccessibilityService, BroadcastReceiver)
-keep class com.example.MainActivity { *; }
-keep class com.example.service.SkipBuddyAccessibilityService { *; }
-keep class com.example.service.BootCompletedReceiver { *; }

# Preserve ViewModel constructors invoked via reflection by ViewModelProvider
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Preserve DataStore Preferences protobuf message fields
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Preserve persisted enum constant names for DataStore compatibility on in-place upgrades
-keepclassmembers enum com.example.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum com.example.detection.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve line numbers for clean crash stack traces if ever needed
-keepattributes SourceFile,LineNumberTable
