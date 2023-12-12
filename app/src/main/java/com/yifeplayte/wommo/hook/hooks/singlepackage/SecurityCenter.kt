package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.AddAOSPAppInfoEntry
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.AddAOSPAppManagerEntry
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.AddOpenByDefaultEntry
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.RemoveAdbInstallIntercept
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.RemoveReportInApplicationInfo
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.SkipCountDown

object SecurityCenter : BasePackage() {
    override val packageName = "com.miui.securitycenter"
    override val hooks = setOf(
        AddAOSPAppManagerEntry,
        AddAOSPAppInfoEntry,
        AddOpenByDefaultEntry,
        RemoveAdbInstallIntercept,
        RemoveReportInApplicationInfo,
        SkipCountDown,
    )
}