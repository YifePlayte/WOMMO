package com.yifeplayte.wommo.hook.hooks

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow
import com.yifeplayte.wommo.utils.ClassScanner.scanObjectOf

abstract class BasePackage {
    private var isInit: Boolean = false
    abstract val packageName: String
    open val hooks: List<BaseHook> = scanObjectOf<BaseHook>(buildString {
        val javaClass = this.javaClass
        append(javaClass.packageName)
        append('.')
        append(javaClass.simpleName.lowercase())
    })

    fun init() {
        if (EzXHelper.hostPackageName != packageName) return
        if (isInit) return
        runCatching {
            hooks.forEach { it.init() }
            isInit = true
            Log.ix("Inited package: ${this.javaClass.simpleName}")
        }.logexIfThrow("Failed init package: ${this.javaClass.simpleName}")
    }
}