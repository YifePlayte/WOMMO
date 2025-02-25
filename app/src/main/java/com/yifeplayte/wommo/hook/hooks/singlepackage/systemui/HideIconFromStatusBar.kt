package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getBoolean

@Suppress("unused")
object HideIconFromStatusBar : BaseHook() {
    override val key = "hide_icon_from_status_bar"
    override val isEnabled
        get() = getBoolean("hide_mobile_signal_icon", false)
                || getBoolean("hide_bluetooth_icon", false)

    override fun hook() {
        @Suppress("UNCHECKED_CAST") val rightBlockList =
            loadClass("com.android.systemui.statusbar.phone.MiuiIconManagerUtils").getDeclaredField(
                "RIGHT_BLOCK_LIST"
            ).get(null) as MutableList<String>
        if (getBoolean("hide_mobile_signal_icon", false)) {
            if (!rightBlockList.contains("mobile")) rightBlockList.add("mobile")
        }
        if (getBoolean("hide_bluetooth_icon", false)) {
            if (!rightBlockList.contains("bluetooth")) rightBlockList.add("bluetooth")
        }
    }
}