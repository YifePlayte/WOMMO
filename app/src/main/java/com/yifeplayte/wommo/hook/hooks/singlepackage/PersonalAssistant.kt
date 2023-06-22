package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.personalassistant.ExposureRefreshForNonMIUIWidget

object PersonalAssistant : BasePackage() {
    override val packageName = "com.miui.personalassistant"
    override val hooks = setOf(
        ExposureRefreshForNonMIUIWidget,
    )
}