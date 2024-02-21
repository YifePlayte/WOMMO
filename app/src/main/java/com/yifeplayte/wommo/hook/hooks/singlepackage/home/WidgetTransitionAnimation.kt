package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object WidgetTransitionAnimation : BaseHook() {
    override val key = "widget_transition_animation"
    override fun hook() {
        loadClass("com.miui.home.launcher.LauncherWidgetView").methodFinder()
            .filterByName("isUseTransitionAnimation").toList().createHooks {
                returnConstant(true)
            }
        loadClass("com.miui.home.launcher.maml.MaMlWidgetView").methodFinder()
            .filterByName("isUseTransitionAnimation").toList().createHooks {
                returnConstant(true)
            }
    }
}