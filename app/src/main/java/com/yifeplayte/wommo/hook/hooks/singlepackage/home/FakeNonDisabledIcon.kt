package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object FakeNonDisabledIcon : BaseHook() {
    override val key = "fake_non_disabled_icon"
    override fun hook() {
        loadClass("com.miui.home.launcher.ItemInfoWithIconAndMessage").findMethod {
            name("isDisabled"); notAbstract()
        }.createHook {
            before { param ->
                if (Thread.currentThread().stackTrace.any { it.methodName == "getColorFilter" }) {
                    param.result = false
                }
            }
        }
    }
}