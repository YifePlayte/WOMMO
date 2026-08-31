package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.annotation.SuppressLint
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClassFirst
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getFloat

@Suppress("unused")
object IconLabel : BaseHook() {
    override val key = "icon_label"
    override val isEnabled = true
    private val labelSize by lazy { getFloat("icon_label_size", 37f) }

    @SuppressLint("DiscouragedApi")
    override fun hook() {
        loadClassFirst(
            "com.miui.home.common.device.DeviceConfigs",
            "com.miui.home.launcher.DeviceConfig",
        ).findMethod { name("getIconTitleTextSize") }.createHook {
            before {
                it.result = labelSize
            }
        }
    }
}