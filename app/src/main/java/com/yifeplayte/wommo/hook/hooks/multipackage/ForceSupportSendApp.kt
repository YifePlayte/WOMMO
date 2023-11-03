package com.yifeplayte.wommo.hook.hooks.multipackage

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.FieldFinder.`-Static`.fieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

object ForceSupportSendApp : BaseMultiHook() {
    override val key = "force_support_send_app"
    override val hooks = mapOf(
        "com.milink.service" to { milink() },
        "com.xiaomi.mirror" to { if (!mirror()) mirrorNew() }
    )

    private fun milink() {
        val clazzMiuiSynergySdk = loadClass("com.xiaomi.mirror.synergy.MiuiSynergySdk")
        clazzMiuiSynergySdk.methodFinder().filterByName("isSupportSendApp").toList().createHooks {
            after {
                it.result = true
            }
        }
    }

    private fun mirror(): Boolean = runCatching {
        val clazzRelayAppMessage = loadClass("com.xiaomi.mirror.message.RelayAppMessage")
        val clazzMiCloudUtils = loadClass("com.xiaomi.mirror.settings.micloud.MiCloudUtils")
        clazzRelayAppMessage.methodFinder().filterByAssignableReturnType(clazzRelayAppMessage)
            .toList().createHooks {
                after {
                    it.result.objectHelper().setObject("isHideIcon", false)
                }
            }
        clazzMiCloudUtils.methodFinder().filterByName("isSupportSubScreen").first().createHook {
            returnConstant(true)
        }
    }.isSuccess

    private fun mirrorNew() {
        val clazzRelayApplication =
            loadClass("com.xiaomi.mirror.message.proto.RelayApp\$RelayApplication")
        clazzRelayApplication.methodFinder().filterByName("getIsHideIcon").first().createHook {
            returnConstant(false)
        }
        clazzRelayApplication.methodFinder().filterByName("getSupportHandOff").first().createHook {
            returnConstant(true)
        }
        clazzRelayApplication.methodFinder().filterByName("getSupportSubScreen").first()
            .createHook {
                returnConstant(true)
            }
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("support_all_app_sub_screen")
                returnType = "boolean"
            }
        }.first().getMethodInstance(safeClassLoader).createHook {
            returnConstant(true)
        }
        val clazzRelayAppMessage = dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("RelayAppMessage{type=", ", isRelay=")
            }
        }.first().getInstance(safeClassLoader)
        clazzRelayAppMessage.let { clazz ->
            val fieldNameIsHideIcon =
                clazz.fieldFinder().filterByType(Boolean::class.javaPrimitiveType!!).toList()
                    .sortedBy { it.name }[1].name
            clazz.methodFinder().filterByReturnType(clazz).toList().createHooks {
                after {
                    setObject(it.result, fieldNameIsHideIcon, false)
                }
            }
        }
    }
}