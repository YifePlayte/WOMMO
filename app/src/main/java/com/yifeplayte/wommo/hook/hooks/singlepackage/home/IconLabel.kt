package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.annotation.SuppressLint
import com.github.kyuubiran.ezxhelper.ClassUtils.loadFirstClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getFloat

@Suppress("unused")
object IconLabel : BaseHook() {
    override val key = "icon_label"
    override val isEnabled = true
    private val labelSize by lazy { getFloat("icon_label_size", 37f) }

    @SuppressLint("DiscouragedApi")
    override fun hook() {
        loadFirstClass(
            "com.miui.home.common.device.DeviceConfigs",
            "com.miui.home.launcher.DeviceConfig",
        ).methodFinder()
            .filterByName("getIconTitleTextSize").single().createHook {
                before {
                    it.result = labelSize
                }
            }
    }
}