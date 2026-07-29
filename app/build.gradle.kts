plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lite.unzipper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lite.unzipper"
        minSdk = 30          // Android 11
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "liteunzipper123"
                keyAlias = "liteunzipper"
                keyPassword = "liteunzipper123"
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            // 启用 R8 代码压缩与资源压缩，显著减小 APK 体积
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile?.exists() == true }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/*.kotlin_module",
                "kotlin/**",
                "META-INF/versions/**"
            )
        }
    }
}

dependencies {
    // AndroidX 基础（精简到必要依赖）
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // 生命周期与 ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 解压核心库：ZIP / 7z / TAR / GZIP / BZIP2 / XZ / AR / CPIO / BR / Zstandard
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("org.tukaani:xz:1.9")
    // Zstandard 解压（更现代的高压缩比格式）
    implementation("com.github.luben:zstd-jni:1.5.5-11@aar")

    // RAR 解压（含 RAR4 / RAR5 / 分卷）
    implementation("com.github.junrar:junrar:7.5.5")
}
