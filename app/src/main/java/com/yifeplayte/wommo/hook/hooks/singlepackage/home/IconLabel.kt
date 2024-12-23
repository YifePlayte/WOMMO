package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.annotation.SuppressLint
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getInt

@Suppress("unused")
object IconLabel : BaseHook() {
    override val key = "icon_label"
    override val isEnabled = true
    private val labelSize by lazy { getInt("icon_label_size", 37).toFloat() }

    @SuppressLint("DiscouragedApi")
    override fun hook() {
        loadClass("com.miui.home.launcher.DeviceConfig").methodFinder()
            .filterByName("getIconTitleTextSize").single().createHook {
                before {
                    it.result = labelSize
                }
            }
    }
}