package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import io.github.lingqiqi5211.ezhooktool.core.findMethod

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
            }.map { it.getMethodInstance() }.createHooks {
                returnConstant(true)
            }
        }
    }
}