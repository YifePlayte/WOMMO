package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

object FakeNonDefaultIcon : BaseHook() {
    override val key = "fake_non_default_icon"
    override fun hook() {
        loadClass("com.miui.home.launcher.DeviceConfig").methodFinder().filterByName("isDefaultIcon").first()
            .createHook {
                returnConstant(false)
            }
    }
}