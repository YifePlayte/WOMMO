package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object HideNavigationBar : BaseHook() {
    override val key = "hide_navigation_bar"
    override fun hook() {
        loadClass("com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.decoration.MiuiDecorationHomeBottom").findMethod {
            name("needCaption"); notAbstract()
        }.createHook {
            returnConstant(false)
        }
        loadClass("com.android.wm.shell.multitasking.miuimultiwinswitch.miuiwindowdecor.decoration.MiuiDecorationBottom").findMethod {
            name("needCaption"); notAbstract()
        }.createHook {
            returnConstant(false)
        }
    }
}