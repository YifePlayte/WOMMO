package com.yifeplayte.wommo.hook.hooks.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.yifeplayte.wommo.hook.hooks.BaseHook

object NotificationSettingsNoWhiteList : BaseHook() {
    override fun init() {
        if (loadClass("miui.os.Build").getField("IS_INTERNATIONAL_BUILD").getBoolean(null)) return
        setStaticObject(
            loadClass("com.android.systemui.statusbar.notification.NotificationSettingsManager"),
            "USE_WHITE_LISTS",
            false
        )
    }
}