package com.yifeplayte.wommo.hook.hooks

import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow
import com.yifeplayte.wommo.utils.ClassScanner.scanObjectOf

abstract class BaseSubPackage {
    private var isInit: Boolean = false
    private lateinit var subClassLoader: ClassLoader
    var safeSubClassLoader
        get() = if (this::subClassLoader.isInitialized) subClassLoader else safeClassLoader
        set(value) {
            if (this::subClassLoader.isInitialized) return
            subClassLoader = value
            initHook()
        }

    abstract val packageName: String
    abstract val subPackageName: String
    open val hooks: List<BaseSubHook> =
        scanObjectOf<BaseSubHook>(javaClass.packageName + "." + javaClass.simpleName.lowercase())

    fun init() {
        if (hostPackageName != packageName) return
        if (isInit) return
        kotlin.runCatching {
            initClassLoader()
        }.logexIfThrow("Failed init sub-package classloader for: $subPackageName in: $packageName")
    }

    private fun initHook() {
        runCatching {
            if (isInit) return
            hooks.forEach { it.init(safeSubClassLoader) }
            isInit = true
            Log.ix("Inited sub-package: ${this.javaClass.simpleName} in: $packageName")
        }.logexIfThrow("Failed init sub-package: ${this.javaClass.simpleName} in: $packageName")
    }

    /**
     * Must call safeSubClassLoader setter
     */
    abstract fun initClassLoader()
}