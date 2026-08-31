package com.yifeplayte.wommo.hook.hooks.singlepackage.getapps

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

import com.yifeplayte.wommo.hook.hooks.BaseHook


@Suppress("unused")
object DisableMarketCustomizeIcon : BaseHook() {
    override val key = "disable_market_customize_icon"
    override fun hook() {
        loadClass($$"com.xiaomi.market.customize_icon.CustomizeIconDataEditor$Companion").findMethod {
            name("isSystemSupportCustomizeIcon"); notAbstract()
        }.createBeforeHook {
            it.result = false
        }
        loadClass("com.xiaomi.market.customize_icon.CustomizeIconDataEditor").findMethod {
            name("isSystemSupportCustomizeIcon"); notAbstract()
        }.createBeforeHook {
            it.result = false
        }
    }
}