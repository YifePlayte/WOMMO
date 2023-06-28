package com.yifeplayte.wommo.hook.hooks

import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getBoolean

abstract class BaseUniversalHook {
    abstract val key: String
    abstract fun hook()
    open val isEnabled: Boolean
        get() = getBoolean(key, false)

    fun init() {
        if (isEnabled) runCatching {
            hook()
            Log.ix("Inited universal hook: ${this.javaClass.simpleName}")
        }.logexIfThrow("Failed init universal hook: ${this.javaClass.simpleName}")
    }
}