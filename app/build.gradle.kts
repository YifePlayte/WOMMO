import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 37
    namespace = "com.yifeplayte.wommo"

    defaultConfig {
        applicationId = "com.yifeplayte.wommo"
        minSdk = 33
        versionCode = 1
        versionName = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())

        ndk {
            abiFilters.add("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=c++_static"
                )
                targets += "wommo_native"
                targets += "classhelper"
            }
        }
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    androidResources {
        additionalParameters += "--allow-reserved-package-id"
        additionalParameters += "--package-id"
        additionalParameters += "0x45"
        generateLocaleConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        prefab = true
    }

    packaging {
        jniLibs.useLegacyPackaging = false
        resources.merges += "META-INF/xposed/*"
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                "WOMMO-${output.versionName.get()}.apk"
            )
        }
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.0")
    debugImplementation("androidx.compose.ui:ui-tooling-preview-android:1.11.4")
    debugImplementation("androidx.compose.ui:ui-tooling:1.11.4")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.compose.foundation:foundation-android:1.11.4")
    implementation("androidx.compose.runtime:runtime-android:1.11.4")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("io.github.lingqiqi5211.ezhooktool:core:1.1.3")
    implementation("io.github.lingqiqi5211.ezhooktool:hook-xposed-102:1.1.3")
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("io.github.ranlee1:jpinyin:1.0.1")
    implementation("me.zhanghai.android.appiconloader:appiconloader:1.5.0")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    implementation("org.luckypray:dexkit:2.2.0")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.3")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
}
