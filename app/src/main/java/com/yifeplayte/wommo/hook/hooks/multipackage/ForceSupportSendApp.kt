package com.yifeplayte.wommo.hook.hooks.multipackage

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseMultiHook

object ForceSupportSendApp : BaseMultiHook() {
    override val hooks: Map<String, () -> Unit> = mapOf(
        "com.milink.service" to { milink() },
        "com.xiaomi.mirror" to { mirror() }
    )

    private fun milink() {
        val clazzMiuiSynergySdk = loadClass("com.xiaomi.mirror.synergy.MiuiSynergySdk")
        clazzMiuiSynergySdk.methodFinder().filterByName("isSupportSendApp").first().createHook {
            after {
                it.result = true
            }
        }
    }

    private fun mirror() {
        val clazzRelayAppMessage = loadClass("com.xiaomi.mirror.message.RelayAppMessage")
        val clazzMiCloudUtils = loadClass("com.xiaomi.mirror.settings.micloud.MiCloudUtils")
        clazzRelayAppMessage.methodFinder().filterByAssignableReturnType(clazzRelayAppMessage).toList().createHooks {
            after {
                it.result.objectHelper().setObject("isHideIcon", false)
            }
        }
        clazzMiCloudUtils.methodFinder().filterByName("isSupportSubScreen").first().createHook {
            returnConstant(true)
        }
    }
}