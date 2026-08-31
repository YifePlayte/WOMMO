package com.yifeplayte.wommo.hook.hooks

import com.yifeplayte.wommo.hook.utils.Log
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getBoolean

abstract class BaseHook {
    private var isInit: Boolean = false
    abstract val key: String
    abstract fun hook()
    open val isEnabled get() = getBoolean(key, false)
    fun init() {
        if (isInit) return
        if (isEnabled) runCatching {
            hook()
            isInit = true
            Log.i("Inited hook: ${this.javaClass.simpleName}")
        }.onFailure {
            Log.e("Failed init hook: ${this.javaClass.simpleName}", it)
        }
    }
}
