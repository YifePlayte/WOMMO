package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object HideLandscapeNavBar : BaseHook() {
    override val key = "hide_landscape_nav_bar"
    override fun hook() {
        loadClass("com.miui.home.recents.views.RecentsContainer").methodFinder().filterByName("hideFakeNavBarForHidingGestureLine")
            .first().createHook {
                before {
                    it.args[0] = true
                }
            }
    }
}