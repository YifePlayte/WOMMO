package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.AddAOSPAppInfoEntry
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.OpenByDefaultSetting
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.RemoveAdbInstallIntercept
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.RemoveReportInApplicationInfo
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.SkipCountDown

object SecurityCenter : BasePackage() {
    override val packageName = "com.miui.securitycenter"
    override val hooks = setOf(
        AddAOSPAppInfoEntry,
        OpenByDefaultSetting,
        RemoveAdbInstallIntercept,
        RemoveReportInApplicationInfo,
        SkipCountDown,
    )
}