package com.yifeplayte.wommo.hook.hooks.multipackage

import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getInstance
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import org.luckypray.dexkit.query.enums.StringMatchType

@Suppress("unused")
object ForceSupportSendApp : BaseMultiHook() {
    override val key = "force_support_send_app"
    override val hooks = mapOf(
        "com.milink.service" to { milink() },
        "com.xiaomi.mirror" to { if (!mirror()) mirrorNew() },
    )

    private fun milink() {
        val clazzMiuiSynergySdk = loadClass("com.xiaomi.mirror.synergy.MiuiSynergySdk")
        clazzMiuiSynergySdk.findAllMethods { name("isSupportSendApp") }.createHooks {
            after {
                it.result = true
            }
        }
    }

    private fun mirror(): Boolean = runCatching {
        val clazzRelayAppMessage = loadClass("com.xiaomi.mirror.message.RelayAppMessage")
        val clazzMiCloudUtils = loadClass("com.xiaomi.mirror.settings.micloud.MiCloudUtils")
        clazzRelayAppMessage.findAllMethods {
            filter { returnType == clazzRelayAppMessage || clazzRelayAppMessage.isAssignableFrom(returnType) || returnType.isAssignableFrom(clazzRelayAppMessage) }
        }.createHooks {
            after {
                (it.result as Any).putField("isHideIcon", false)
            }
        }
        clazzMiCloudUtils.findMethod { name("isSupportSubScreen") }.createHook {
            returnConstant(true)
        }
    }.isSuccess

    private fun mirrorNew() {
        val clazzRelayApplication =
            loadClass($$"com.xiaomi.mirror.message.proto.RelayApp$RelayApplication")
        clazzRelayApplication.findMethod { name("getIsHideIcon"); notAbstract() }.createHook {
            returnConstant(false)
        }
        clazzRelayApplication.findMethod { name("getSupportHandOff"); notAbstract() }.createHook {
            returnConstant(true)
        }
        clazzRelayApplication.findMethod { name("getSupportSubScreen"); notAbstract() }.createHook {
            returnConstant(true)
        }
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("support_all_app_sub_screen")
                returnType = "boolean"
            }
        }.single().getMethodInstance().createHook {
            returnConstant(true)
        }
        val clazzRelayAppMessage = dexKitBridge.findClass {
            matcher {
                usingStrings(
                    listOf("RelayAppMessage{type='", ", isRelay='"),
                    StringMatchType.Equals
                )
            }
        }.single().getInstance()
        clazzRelayAppMessage.let { clazz ->
            val fieldNameIsHideIcon =
                clazz.declaredFields.filter { it.type == Boolean::class.javaPrimitiveType }
                    .sortedBy { it.name }[1].name
            clazz.findAllMethods { returnType(clazz) }.createHooks {
                after {
                    (it.result as Any).putField(fieldNameIsHideIcon, false)
                }
            }
        }
        runCatching {
            loadClass("com.xiaomi.mirror.message.RelayAppMessage").let { clazz ->
                clazz.findAllMethods { returnType(clazz) }.createHooks {
                    after {
                        (it.result as Any).putField("isHideIcon", false)
                    }
                }
            }
        }
    }
}
