package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object WidgetTransitionAnimation : BaseHook() {
    override val key = "widget_transition_animation"
    override fun hook() {
        loadClass("com.miui.home.launcher.LauncherWidgetView").findAllMethods {
            name("isUseTransitionAnimation")
        }.createHooks {
            returnConstant(true)
        }
        loadClass("com.miui.home.launcher.maml.MaMlWidgetView").findAllMethods {
            name("isUseTransitionAnimation")
        }.createHooks {
            returnConstant(true)
        }
    }
}