package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

@Suppress("unused")
object RemoveReportInApplicationInfo : BaseHook() {
    override val key = "remove_report_in_application_info"
    override fun hook() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("com.xiaomi.market")
                declaredClass = "com.miui.appmanager.ApplicationsDetailsActivity"
            }
        }.singleOrNull()?.getMethodInstance(safeClassLoader)?.createHook {
            returnConstant(false)
        }
    }
}