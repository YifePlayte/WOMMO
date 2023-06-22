package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.OpenByDefaultSetting

object SecurityCenter : BasePackage() {
    override val packageName = "com.miui.securitycenter"
    override val hooks = setOf(
        OpenByDefaultSetting,
    )
}