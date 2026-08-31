package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object HideLandscapeNavBar : BaseHook() {
    override val key = "hide_landscape_nav_bar"
    override fun hook() {
        loadClass("com.miui.home.recents.views.RecentsContainer").findMethod {
            name("hideFakeNavBarForHidingGestureLine")
        }.createHook {
            before {
                it.args[0] = true
            }
        }
    }
}