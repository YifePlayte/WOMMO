package com.yifeplayte.wommo.hook.hooks.singlepackage.googleplayservices

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

@Suppress("unused")
object FakeHotspotDisabledForQuickShare : BaseHook() {
    override val key = "fake_hotspot_disabled_for_quick_share"
    override fun hook() {
        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf(
                    "SetRadioStatus(isBluetoothEnabled=",
                    ", isHotspotEnabled=",
                )
            }
        }.single().getInstance(safeClassLoader).declaredConstructors.single().createHook {
            before { param ->
                param.args[5] = false
            }
        }
    }
}