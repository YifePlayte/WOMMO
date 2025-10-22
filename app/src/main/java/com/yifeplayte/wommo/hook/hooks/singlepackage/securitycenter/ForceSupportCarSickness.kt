package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

@Suppress("unused")
object ForceSupportCarSickness : BaseHook() {
    override val key = "force_support_car_sickness"
    override fun hook() {
        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("is_support_carsick")
            }
        }.singleOrNull()?.name?.let { className ->
            dexKitBridge.findMethod {
                matcher {
                    usingStrings = listOf("sensor")
                    declaredClass = className
                    returnType = "boolean"
                }
            }.map { it.getMethodInstance(safeClassLoader) }.createHooks {
                returnConstant(true)
            }
        }
    }
}