# UsagiKit Consumer Proguard Rules
-keep class tsuki.** { *; }
-keep interface tsuki.** { *; }
-keep class org.draken.usagikit.** { *; }
-keepclassmembers class * implements tsuki.MangaParser {
    public <init>(...);
}
-keepattributes *Annotation*
