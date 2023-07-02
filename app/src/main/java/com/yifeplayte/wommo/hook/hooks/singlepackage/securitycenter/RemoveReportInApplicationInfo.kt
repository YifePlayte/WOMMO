package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.loadDexKit

object RemoveReportInApplicationInfo : BaseHook() {
    override val key = "remove_report_in_application_info"
    override fun hook() {
        loadDexKit()
        dexKitBridge.findMethodUsingString {
            usingString = "com.xiaomi.market"
            methodDeclareClass = "com.miui.appmanager.ApplicationsDetailsActivity"
        }.firstOrNull()?.getMethodInstance(safeClassLoader)?.createHook {
            returnConstant(false)
        }
    }
}