package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.yifeplayte.wommo.hook.hooks.BaseSingleHook

object NotificationSettingsNoWhiteList : BaseSingleHook() {
    override fun init() {
        if (getStaticObjectOrNullAs<Boolean>(loadClass("miui.os.Build"), "IS_INTERNATIONAL_BUILD") != true) return
        setStaticObject(
            loadClass("com.android.systemui.statusbar.notification.NotificationSettingsManager"),
            "USE_WHITE_LISTS",
            false
        )
    }
}