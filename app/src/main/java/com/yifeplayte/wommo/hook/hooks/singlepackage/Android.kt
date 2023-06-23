package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.android.ForceDarkModeForAllApps

object Android : BasePackage() {
    override val packageName = "android"
    override val hooks = setOf(
        ForceDarkModeForAllApps,
    )
}