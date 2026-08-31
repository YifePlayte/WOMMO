package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import io.github.lingqiqi5211.ezhooktool.core.findMethod

@Suppress("unused")
object RemoveReportInApplicationInfo : BaseHook() {
    override val key = "remove_report_in_application_info"
    override fun hook() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("com.xiaomi.market")
                declaredClass = "com.miui.appmanager.ApplicationsDetailsActivity"
            }
        }.singleOrNull()?.getMethodInstance()?.createHook {
            returnConstant(false)
        }
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("com.xiaomi.market")
                declaredClass = "com.miui.appmanager.fragment.ApplicationsDetailsFragment"
            }
        }.singleOrNull()?.getMethodInstance()?.createHook {
            returnConstant(false)
        }
    }
}