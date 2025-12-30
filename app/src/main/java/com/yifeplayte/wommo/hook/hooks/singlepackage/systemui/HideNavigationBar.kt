package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object HideNavigationBar : BaseHook() {
    override val key = "hide_navigation_bar"
    override fun hook() {
        loadClass("com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.decoration.MiuiDecorationHomeBottom").methodFinder()
            .filterByName("needCaption").filterNonAbstract().single().createHook {
                returnConstant(false)
            }
        loadClass("com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.decoration.MiuiDecorationBottom").methodFinder()
            .filterByName("needCaption").filterNonAbstract().single().createHook {
                returnConstant(false)
            }
    }
}