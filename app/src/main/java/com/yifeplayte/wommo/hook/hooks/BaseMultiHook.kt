package com.yifeplayte.wommo.hook.hooks

import com.github.kyuubiran.ezxhelper.EzXHelper

abstract class BaseMultiHook {
    var isInit: Boolean = false
    abstract val hooks: Map<String, () -> Unit>
    fun init() {
        hooks[EzXHelper.hostPackageName]?.invoke()
    }
}