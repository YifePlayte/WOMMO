package com.yifeplayte.wommo.hook.hooks.systemui

import com.github.kyuubiran.ezxhelper.ClassHelper.Companion.classHelper
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.yifeplayte.wommo.hook.hooks.BaseHook
import de.robv.android.xposed.XposedHelpers

object NotificationSettingsNoWhiteList : BaseHook() {
    override fun init() {
        if (loadClass("miui.os.Build").classHelper()
                .getStaticObjectOrNullAs<Boolean>("IS_INTERNATIONAL_BUILD")!!
        ) return
        XposedHelpers.setStaticObjectField(
            loadClass("com.android.systemui.statusbar.notification.NotificationSettingsManager"),
            "USE_WHITE_LISTS",
            false
        )
    }
}