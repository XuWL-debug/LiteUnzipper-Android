# R8 / ProGuard 规则

# 解压库保留
-keep class org.apache.commons.compress.** { *; }
-keep class com.github.junrar.** { *; }
-keep class org.tukaani.xz.** { *; }
-keep class com.github.luben.zstd.** { *; }

# 保留原生方法（zstd-jni 等使用 JNI）
-keepclasseswithmembernames class * {
    native <methods>;
}

# 忽略 commons-compress 中未使用的可选依赖（ASM、SLF4J、commons-codec、brotli）
-dontwarn org.objectweb.asm.**
-dontwarn org.slf4j.impl.**
-dontwarn org.slf4j.**
-dontwarn org.apache.commons.codec.**
-dontwarn org.brotli.dec.**

# Pack200 与 Harmony 相关类引用 ASM，但我们不使用 Pack200，可安全忽略
-dontwarn org.apache.commons.compress.harmony.pack200.**
-dontwarn org.apache.commons.compress.compressors.brotli.**
-dontwarn org.apache.commons.compress.compressors.snappy.**
-dontwarn org.apache.commons.compress.compressors.lz4.**

# AndroidX 与 Material 已自带 consumer rules，无需重复

# 优化：移除日志调用（仅 Release）
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
