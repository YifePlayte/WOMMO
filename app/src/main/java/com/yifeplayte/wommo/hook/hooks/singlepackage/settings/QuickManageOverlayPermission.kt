package com.yifeplayte.wommo.hook.hooks.singlepackage.settings

import android.app.Activity
import android.provider.Settings
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object QuickManageOverlayPermission : BaseHook() {
    override val key = "quick_manage_overlay_permission"
    override fun hook() {
        loadClass("com.android.settings.SettingsActivity").methodFinder().filterByName("redirectTabletActivity").first().createHook {
            before {
                val activity = it.thisObject as Activity
                val intent = activity.intent
                val action = intent.action
                val data = intent.data
                if (action != Settings.ACTION_MANAGE_OVERLAY_PERMISSION || data == null || data.scheme != "package") return@before
                activity.objectHelper()
                    .setObjectUntilSuperclass("initialFragmentName", "com.android.settings.applications.appinfo.DrawOverlayDetails")
            }
        }
    }
}