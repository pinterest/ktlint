-dontobfuscate
-keepattributes SourceFile, LineNumberTable

-allowaccessmodification

-keep class com.pinterest.ktlint.Main {
  public static void main(java.lang.String[]);
}

-dontwarn com.sun.jna.**
-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn kotlin.annotations.jvm.**
-dontwarn kotlinx.coroutines.**
-dontwarn org.checkerframework.checker.nullness.qual.**
-dontwarn org.codehaus.janino.ClassBodyEvaluator
-dontwarn org.jetbrains.annotations.**
-dontwarn org.jetbrains.kotlin.com.google.**
