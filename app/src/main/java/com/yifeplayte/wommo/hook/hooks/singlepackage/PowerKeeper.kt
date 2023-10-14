package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.powerkeeper.EnableBatteryMonitorService

object PowerKeeper : BasePackage() {
    override val packageName = "com.miui.powerkeeper"
    override val hooks = setOf(
        EnableBatteryMonitorService,
    )
}