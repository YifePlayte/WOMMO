package com.yifeplayte.wommo.hook.hooks

import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getBoolean

abstract class BaseUniversalHook {
    abstract val key: String
    abstract fun hook()
    open val isEnabled get() = getBoolean(key, false)
    open val isPartialUniversalHook: Boolean = false
    open val scope: Set<String>? = null
    fun init() {
        if (isEnabled) runCatching {
            if (isPartialUniversalHook) {
                if (scope == null) return
                if (hostPackageName !in scope!!) return
            }
            hook()
            Log.ix("Inited universal hook: ${this.javaClass.simpleName}")
        }.logexIfThrow("Failed init universal hook: ${this.javaClass.simpleName}")
    }
}