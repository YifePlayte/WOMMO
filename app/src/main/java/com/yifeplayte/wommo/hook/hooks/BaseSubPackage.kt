package com.yifeplayte.wommo.hook.hooks

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow

abstract class BaseSubPackage {
    var isInit: Boolean = false
    lateinit var subClassLoader: ClassLoader
    var safeSubClassLoader
        get() = if (this::subClassLoader.isInitialized) subClassLoader else EzXHelper.safeClassLoader
        set(value) {
            if (this::subClassLoader.isInitialized) return
            subClassLoader = value
            initHook()
        }

    abstract val packageName: String
    abstract val subPackageName: String
    abstract val hooks: Set<BaseSubHook>
    fun init() {
        if (EzXHelper.hostPackageName != packageName) return
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