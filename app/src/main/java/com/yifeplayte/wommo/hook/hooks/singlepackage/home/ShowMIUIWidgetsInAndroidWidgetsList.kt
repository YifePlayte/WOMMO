package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object ShowMIUIWidgetsInAndroidWidgetsList : BaseHook() {
    override val key = "show_miui_widgets_in_android_widgets_list"
    override fun hook() {
        loadClass("com.miui.home.launcher.MIUIWidgetUtil").methodFinder()
            .filterByName("isMIUIWidgetSupport").single().createHook {
            after { param ->
                if (Thread.currentThread().stackTrace.any {
                        it.className in setOf(
                            "com.miui.home.launcher.widget.WidgetsVerticalAdapter",
                            "com.miui.home.launcher.widget.BaseWidgetsVerticalAdapter"
                        )
                    }) {
                    param.result = false
                }
            }
        }
    }
}