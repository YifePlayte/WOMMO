package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getBoolean

@Suppress("unused")
object ManageIconOnStatusBar : BaseHook() {
    override val key = "manage_icon_on_status_bar"
    override val isEnabled
        get() = getBoolean("hide_mobile_signal_icon", false)
                || getBoolean("hide_bluetooth_icon", false)
                || getBoolean("show_bluetooth_handsfree_battery_icon", false)

    override fun hook() {
        val clazzMiuiIconManagerUtils =
            loadClass("com.android.systemui.statusbar.phone.MiuiIconManagerUtils")
        @Suppress("UNCHECKED_CAST") val rightBlockList =
            clazzMiuiIconManagerUtils.getDeclaredField(
                "RIGHT_BLOCK_LIST"
            ).get(null) as MutableList<String>
        @Suppress("UNCHECKED_CAST") val miniRightBlockList =
            clazzMiuiIconManagerUtils.getDeclaredField(
                "MINI_RIGHT_BLOCK_LIST"
            ).get(null) as MutableList<String>

        if (getBoolean("hide_mobile_signal_icon", false)) {
            addIfAbsent("mobile", rightBlockList, miniRightBlockList)
        }
        if (getBoolean("hide_bluetooth_icon", false)) {
            addIfAbsent("bluetooth", rightBlockList, miniRightBlockList)
        }
        if (getBoolean("show_bluetooth_handsfree_battery_icon", false)) {
            removeIfExists("bluetooth_handsfree_battery", rightBlockList, miniRightBlockList)
        }
    }

    fun addIfAbsent(element: String, vararg lists: MutableList<String>) {
        lists.forEach { list ->
            if (!list.contains(element)) {
                list.add(element)
            }
        }
    }

    fun removeIfExists(element: String, vararg lists: MutableList<String>) {
        lists.forEach { list ->
            if (list.contains(element)) {
                list.remove(element)
            }
        }
    }
}