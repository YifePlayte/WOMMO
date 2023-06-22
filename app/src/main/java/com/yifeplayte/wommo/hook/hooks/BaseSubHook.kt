package com.yifeplayte.wommo.hook.hooks

import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getBoolean

abstract class BaseSubHook {
    var isInit: Boolean = false
    abstract val key: String
    abstract fun hook(subClassLoader: ClassLoader)
    val isEnabled: Boolean
        get() = getBoolean(key, false)

    fun init(subClassLoader: ClassLoader) {
        if (isInit) return
        if (isEnabled) runCatching {
            hook(subClassLoader)
            isInit = true
            Log.ix("Inited hook: ${this.javaClass.simpleName}")
        }.logexIfThrow("Failed init hook: ${this.javaClass.simpleName}")
    }
}