package com.yifeplayte.wommo.hook.hooks

import com.yifeplayte.wommo.hook.utils.Log
import com.yifeplayte.wommo.hook.utils.isHostPackage
import com.yifeplayte.wommo.utils.ClassScanner.scanObjectOf
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed

abstract class BaseSubPackage(
    val packageName: String,
    val subPackageName: String
) {
    private var isInit: Boolean = false
    private lateinit var subClassLoader: ClassLoader
    var safeSubClassLoader
        get() = if (this::subClassLoader.isInitialized) subClassLoader else EzXposed.safeClassLoader
        set(value) {
            if (this::subClassLoader.isInitialized) return
            subClassLoader = value
            initHook()
        }
    open val hooks: List<BaseSubHook> by lazy {
        scanObjectOf<BaseSubHook>(javaClass.packageName + "." + javaClass.simpleName.lowercase())
    }

    fun init() {
        if (!isHostPackage(packageName)) return
        if (isInit) return
        kotlin.runCatching {
            initClassLoader()
        }.onFailure {
            Log.e("Failed init sub-package classloader for: $subPackageName in: $packageName", it)
        }
    }

    private fun initHook() {
        runCatching {
            if (isInit) return
            hooks.forEach { it.init(safeSubClassLoader) }
            isInit = true
            Log.i("Inited sub-package: ${this.javaClass.simpleName} in: $packageName")
        }.onFailure {
            Log.e("Failed init sub-package: ${this.javaClass.simpleName} in: $packageName", it)
        }
    }

    /**
     * Must call safeSubClassLoader setter
     */
    abstract fun initClassLoader()
}
