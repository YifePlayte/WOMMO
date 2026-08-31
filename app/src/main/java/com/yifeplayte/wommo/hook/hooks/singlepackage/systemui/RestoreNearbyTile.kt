package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD
import com.yifeplayte.wommo.utils.Clazz.setStaticFinalObject
import io.github.lingqiqi5211.ezhooktool.core.findMethod

@Suppress("unused")
object RestoreNearbyTile : BaseHook() {
    override val key = "restore_near_by_tile"
    override val isEnabled get() = !IS_INTERNATIONAL_BUILD && super.isEnabled
    override fun hook() {
        val clazzMiuiConfigs = loadClass("com.miui.utils.configs.MiuiConfigs")

        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("com.google.android.gms/.nearby.sharing.SharingTileService")
            }
        }.map { it.getMethodInstance() }.createHooks {
            before {
                setStaticFinalObject(clazzMiuiConfigs, "IS_INTERNATIONAL_BUILD", true)
            }
            after {
                setStaticFinalObject(clazzMiuiConfigs, "IS_INTERNATIONAL_BUILD", false)
            }
        }
    }
}