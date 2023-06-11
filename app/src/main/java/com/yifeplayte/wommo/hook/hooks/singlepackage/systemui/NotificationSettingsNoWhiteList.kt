package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.yifeplayte.wommo.hook.hooks.BaseSingleHook
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD

object NotificationSettingsNoWhiteList : BaseSingleHook() {
    override fun init() {
        if (!IS_INTERNATIONAL_BUILD) return
        setStaticObject(
            loadClass("com.android.systemui.statusbar.notification.NotificationSettingsManager"),
            "USE_WHITE_LISTS",
            false
        )
    }
}