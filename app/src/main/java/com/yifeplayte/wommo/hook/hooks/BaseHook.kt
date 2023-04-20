package com.yifeplayte.wommo.hook.hooks

abstract class BaseHook {
    var isInit: Boolean = false
    abstract fun init()
}