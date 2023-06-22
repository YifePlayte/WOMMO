package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.AllowMoveNonMIUIWidgetToMinusScreen
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.RestoreGoogleAppIcon
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.RestoreSwitchMinusScreen

object Home : BasePackage() {
    override val packageName = "com.miui.home"
    override val hooks = setOf(
        AllowMoveNonMIUIWidgetToMinusScreen,
        RestoreGoogleAppIcon,
        RestoreSwitchMinusScreen,
    )
}