package com.yifeplayte.wommo.hook.hooks

abstract class BaseSingleHook {
    var isInit: Boolean = false
    abstract fun init()
}