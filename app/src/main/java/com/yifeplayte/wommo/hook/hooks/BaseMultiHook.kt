package com.yifeplayte.wommo.hook.hooks

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getBoolean

abstract class BaseMultiHook {
    private var isInit: Boolean = false
    abstract val key: String
    abstract val hooks: Map<String, () -> Unit>
    open val isEnabled get() = getBoolean(key, false)
    fun init() {
        if (isInit) return
        if (!isEnabled) return
        hooks[EzXHelper.hostPackageName]?.runCatching {
            invoke()
            isInit = true
            Log.ix("Inited hook: ${this@BaseMultiHook.javaClass.simpleName} in: ${EzXHelper.hostPackageName}")
        }?.logexIfThrow("Failed init hook: ${this@BaseMultiHook.javaClass.simpleName} in: ${EzXHelper.hostPackageName}")
    }
}