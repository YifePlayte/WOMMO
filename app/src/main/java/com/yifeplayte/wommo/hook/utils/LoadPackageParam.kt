package com.yifeplayte.wommo.hook.utils

import android.os.Build
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import java.io.File

/**
 * 宿主包信息工具
 *
 * 传入包名与 apk 路径，不依赖 libxposed 类型，可在任意上下文使用
 */
@Suppress("unused")
object LoadPackageParam {
    /**
     * 获取被 hook 应用的版本号
     * 当被 hook 的为系统框架时，返回 Android SDK版本号
     */
    fun getAppVersionCode(packageName: String, sourceDir: String): Int = runCatching {
        if (packageName == "system") {
            Build.VERSION.SDK_INT
        } else {
            val parser = loadClass("android.content.pm.PackageParser").getConstructor().newInstance()
            val apkPath = File(sourceDir)
            val pkg = parser.callMethod("parsePackage", apkPath, 0)
            pkg?.getFieldAs<Int>("mVersionCode") ?: 0
        }
    }.getOrDefault(0)

    /**
     * 获取被 hook 应用的版本名称
     * 当被 hook 的为系统框架时，返回 Android 版本号或版本名称
     */
    fun getAppVersionName(packageName: String, sourceDir: String): String = runCatching {
        if (packageName == "system") {
            Build.VERSION.RELEASE_OR_CODENAME
        } else {
            val parser = loadClass("android.content.pm.PackageParser").getConstructor().newInstance()
            val apkPath = File(sourceDir)
            val pkg = parser.callMethod("parsePackage", apkPath, 0)
            pkg?.getFieldAs<String>("mVersionName") ?: "Error: Unknown"
        }
    }.getOrDefault("Error: Unknown")
}
