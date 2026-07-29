pluginManagement {
    repositories {
        // 阿里云镜像（国内加速）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        // JitPack 镜像（zstd-jni 等）
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://jitpack.io") }
        google()
        mavenCentral()
    }
}

rootProject.name = "LiteUnzipper"
include(":app")
