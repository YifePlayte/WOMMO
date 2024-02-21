package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD

@Suppress("unused")
object RestoreNearbyTile : BaseHook() {
    override val key = "restore_near_by_tile"
    override val isEnabled get() = !IS_INTERNATIONAL_BUILD and super.isEnabled
    override fun hook() {
        val clazzMiuiConfigs = loadClass("com.miui.utils.configs.MiuiConfigs")

        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("com.google.android.gms/.nearby.sharing.SharingTileService")
            }
        }.map { it.getMethodInstance(safeClassLoader) }.createHooks {
            before {
                setStaticObject(clazzMiuiConfigs, "IS_INTERNATIONAL_BUILD", true)
            }
            after {
                setStaticObject(clazzMiuiConfigs, "IS_INTERNATIONAL_BUILD", false)
            }
        }
    }
}