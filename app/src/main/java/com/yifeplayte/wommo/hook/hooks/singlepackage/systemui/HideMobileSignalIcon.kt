package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object HideMobileSignalIcon : BaseHook() {
    override val key = "hide_mobile_signal_icon"
    override fun hook() {
        @Suppress("UNCHECKED_CAST") val rightBlockList =
            loadClass("com.android.systemui.statusbar.phone.MiuiIconManagerUtils").getDeclaredField(
                "RIGHT_BLOCK_LIST"
            ).get(null) as MutableList<String>
        if (!rightBlockList.contains("mobile")) rightBlockList.add("mobile")
    }
}