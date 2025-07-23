import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 36
    namespace = "com.yifeplayte.wommo"

    defaultConfig {
        applicationId = "com.yifeplayte.wommo"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }

        splits {
            abi {
                isEnable = true
                reset()
                include("armeabi-v7a", "arm64-v8a")
                isUniversalApk = true
            }
        }

        applicationVariants.configureEach {
            outputs.configureEach {
                if (this is BaseVariantOutputImpl) {
                    outputFileName = outputFileName.replace("app", rootProject.name)
                        .replace(Regex("debug|release"), versionName)
                }
            }
        }
    }

    buildTypes {
        named("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
        named("debug") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
            versionNameSuffix = "-debug-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(LocalDateTime.now())
        }
    }

    androidResources {
        additionalParameters += "--allow-reserved-package-id"
        additionalParameters += "--package-id"
        additionalParameters += "0x45"
        generateLocaleConfig = true
    }

    kotlin {
        jvmToolchain(21)
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/classhelper/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    debugImplementation("androidx.compose.ui:ui-tooling-preview-android:1.8.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.8.3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.compose.foundation:foundation-android:1.8.3")
    implementation("androidx.compose.runtime:runtime-android:1.8.3")
    implementation("androidx.navigation:navigation-compose:2.9.2")
    implementation("com.github.kyuubiran:EzXHelper:2.2.1")
    implementation("dev.chrisbanes.haze:haze-android:1.6.9")
    implementation("io.github.ranlee1:jpinyin:1.0.1")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    implementation("org.luckypray:dexkit:2.0.6")
    implementation("top.yukonga.miuix.kmp:miuix-android:0.4.7")
}
