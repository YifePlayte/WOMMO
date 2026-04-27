package com.yifeplayte.wommo.hook.hooks.singlepackage.getapps

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook


@Suppress("unused")
object DisableMarketCustomizeIcon : BaseHook() {
    override val key = "disable_market_customize_icon"
    override fun hook() {
        loadClass($$"com.xiaomi.market.customize_icon.CustomizeIconDataEditor$Companion").methodFinder()
            .filterByName("isSystemSupportCustomizeIcon").filterNonAbstract().single().createBeforeHook {
                it.result = false
            }
        loadClass("com.xiaomi.market.customize_icon.CustomizeIconDataEditor").methodFinder()
            .filterByName("isSystemSupportCustomizeIcon").filterNonAbstract().single().createBeforeHook {
                it.result = false
            }
    }
}