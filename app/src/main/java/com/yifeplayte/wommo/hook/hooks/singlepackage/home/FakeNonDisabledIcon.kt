package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object FakeNonDisabledIcon : BaseHook() {
    override val key = "fake_non_disabled_icon"
    override fun hook() {
        loadClass("com.miui.home.launcher.ItemInfoWithIconAndMessage").methodFinder()
            .filterByName("isDisabled").filterNonAbstract().single().createHook {
                before { param ->
                    if (Thread.currentThread().stackTrace.any { it.methodName == "getColorFilter" }) {
                        param.result = false
                    }
                }
            }
    }
}