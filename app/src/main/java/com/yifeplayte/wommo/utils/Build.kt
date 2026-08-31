package com.yifeplayte.wommo.utils

import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.getStaticFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass

/**
 * 获取系统信息
 *
 * 在模块 UI 进程与 hook 进程中都会使用；
 * miui.os.Build / SystemProperties 均为框架类，core 的 loadClass 默认 ClassLoader 即可解析
 */
@Suppress("unused")
object Build {
    private val clazzMiuiBuild by lazy {
        loadClass("miui.os.Build")
    }

    private val clazzSystemProperties by lazy {
        loadClass("android.os.SystemProperties")
    }

    /**
     * 设备是否为平板
     */
    val IS_TABLET by lazy {
        runCatching { clazzMiuiBuild.getStaticFieldOrNullAs<Boolean>("IS_TABLET") }.getOrNull() ?: false
    }

    /**
     * 是否为国际版系统
     */
    val IS_INTERNATIONAL_BUILD by lazy {
        runCatching { clazzMiuiBuild.getStaticFieldOrNullAs<Boolean>("IS_INTERNATIONAL_BUILD") }.getOrNull() ?: false
    }

    private val hyperOsVersionCode: Int by lazy {
        runCatching {
            clazzSystemProperties.callStaticMethod("getInt", "ro.mi.os.version.code", -1) as? Int
        }.getOrNull() ?: -1
    }

    /**
     * 是否为HyperOS
     */
    val IS_HYPER_OS by lazy {
        hyperOsVersionCode != -1
    }

    /**
     * HyperOS版本
     */
    val HYPER_OS_VERSION by lazy {
        hyperOsVersionCode
    }

    /**
     * 是否支持超级岛
     */
    val IS_SUPPORT_ISLAND by lazy {
        runCatching {
            clazzSystemProperties.callStaticMethod("getBoolean", "persist.sys.feature.island", false) as? Boolean
        }.getOrNull() ?: false
    }
}
