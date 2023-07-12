package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.AllowMoveNonMIUIWidgetToMinusScreen
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.EnablePerfectIcons
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.FakeNonDefaultIcon
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.RestoreGoogleAppIcon
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.RestoreSwitchMinusScreen
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.WidgetTransitionAnimation

object Home : BasePackage() {
    override val packageName = "com.miui.home"
    override val hooks = setOf(
        AllowMoveNonMIUIWidgetToMinusScreen,
        EnablePerfectIcons,
        FakeNonDefaultIcon,
        RestoreGoogleAppIcon,
        RestoreSwitchMinusScreen,
        WidgetTransitionAnimation,
    )
}