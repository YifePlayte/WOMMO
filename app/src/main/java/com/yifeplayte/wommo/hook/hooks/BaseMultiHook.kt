package com.yifeplayte.wommo.hook.hooks

import com.yifeplayte.wommo.hook.utils.Log
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getBoolean
import com.yifeplayte.wommo.hook.utils.hostPackageName

abstract class BaseMultiHook {
    private var isInit: Boolean = false
    abstract val key: String
    abstract val hooks: Map<String, () -> Unit>
    open val isEnabled get() = getBoolean(key, false)
    fun init() {
        if (isInit) return
        if (!isEnabled) return
        val hook = hooks[hostPackageName] ?: return
        runCatching {
            hook()
            isInit = true
            Log.i("Inited hook: ${this@BaseMultiHook.javaClass.simpleName} in: $hostPackageName")
        }.onFailure {
            Log.e("Failed init hook: ${this@BaseMultiHook.javaClass.simpleName} in: $hostPackageName", it)
        }
    }
}
