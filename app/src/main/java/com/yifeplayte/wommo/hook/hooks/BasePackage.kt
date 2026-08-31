package com.yifeplayte.wommo.hook.hooks

import com.yifeplayte.wommo.hook.utils.Log
import com.yifeplayte.wommo.hook.utils.isHostPackage
import com.yifeplayte.wommo.utils.ClassScanner.scanObjectOf

abstract class BasePackage(val packageName: String) {
    private var isInit: Boolean = false
    open val hooks: List<BaseHook> by lazy {
        scanObjectOf<BaseHook>(javaClass.packageName + "." + javaClass.simpleName.lowercase())
    }

    fun init() {
        if (!isHostPackage(packageName)) return
        if (isInit) return
        runCatching {
            hooks.forEach { it.init() }
            isInit = true
            Log.i("Inited package: ${this.javaClass.simpleName}")
        }.onFailure {
            Log.e("Failed init package: ${this.javaClass.simpleName}", it)
        }
    }
}
