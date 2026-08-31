package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object ShowMIUIWidgetsInAndroidWidgetsList : BaseHook() {
    override val key = "show_miui_widgets_in_android_widgets_list"
    override fun hook() {
        loadClass("com.miui.home.launcher.MIUIWidgetUtil").findMethod {
            name("isMIUIWidgetSupport")
        }.createHook {
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