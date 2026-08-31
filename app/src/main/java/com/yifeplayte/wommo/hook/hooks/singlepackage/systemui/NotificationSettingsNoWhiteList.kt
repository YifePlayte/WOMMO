package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putStaticField
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD

@Suppress("unused")
object NotificationSettingsNoWhiteList : BaseHook() {
    override val key = "notification_settings_no_white_list"
    override val isEnabled get() = !IS_INTERNATIONAL_BUILD && super.isEnabled
    override fun hook() {
        loadClass("com.android.systemui.statusbar.notification.NotificationSettingsManager").putStaticField(
            "USE_WHITE_LISTS",
            false
        )
    }
}