package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.settings.ShowNotificationImportance

object Settings : BasePackage() {
    override val packageName = "com.android.settings"
    override val hooks = setOf(
        ShowNotificationImportance,
    )
}