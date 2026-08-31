package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import io.github.lingqiqi5211.ezhooktool.core.findMethod

@Suppress("unused")
object RemoveAdbInstallIntercept : BaseHook() {
    override val key = "remove_adb_install_intercept"
    override fun hook() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("permcenter_install_intercept_enabled")
                returnType = "boolean"
            }
        }.single().getMethodInstance().createHook {
            returnConstant(false)
        }
    }
}