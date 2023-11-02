package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

object RemoveAdbInstallIntercept : BaseHook() {
    override val key = "remove_adb_install_intercept"
    override fun hook() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("permcenter_install_intercept_enabled")
                returnType = "boolean"
            }
        }.first().getMethodInstance(safeClassLoader).createHook {
            returnConstant(false)
        }
    }
}