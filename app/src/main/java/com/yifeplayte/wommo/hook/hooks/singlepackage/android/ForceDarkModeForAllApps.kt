package com.yifeplayte.wommo.hook.hooks.singlepackage.android

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD


object ForceDarkModeForAllApps : BaseHook() {
    override val key = "force_dark_mode_for_all_apps"
    override fun hook() {
        if (IS_INTERNATIONAL_BUILD) return
        loadClass("com.android.server.ForceDarkAppListManager").methodFinder().filterByName("getDarkModeAppList")
            .toList().createHooks {
                before {
                    setStaticObject(loadClass("miui.os.Build"), "IS_INTERNATIONAL_BUILD", true)
                }
                after {
                    setStaticObject(loadClass("miui.os.Build"), "IS_INTERNATIONAL_BUILD", IS_INTERNATIONAL_BUILD)
                }
            }
    }
}