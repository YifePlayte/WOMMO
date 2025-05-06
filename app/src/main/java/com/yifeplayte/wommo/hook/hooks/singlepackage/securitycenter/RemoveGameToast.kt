package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

@Suppress("unused")
object RemoveGameToast : BaseHook() {
    override val key = "remove_game_toast"
    override fun hook() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("showWildModeToastView: ")
            }
        }.single().getMethodInstance(safeClassLoader).createHook {
            returnConstant(null)
        }
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("cancel game toast , isCanceled : ")
            }
        }.single().getMethodInstance(safeClassLoader).createHook {
            returnConstant(null)
        }
    }
}