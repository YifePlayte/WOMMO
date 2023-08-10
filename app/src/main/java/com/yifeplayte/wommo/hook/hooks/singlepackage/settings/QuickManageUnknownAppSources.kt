package com.yifeplayte.wommo.hook.hooks.singlepackage.settings

import android.app.Activity
import android.provider.Settings
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

object QuickManageUnknownAppSources : BaseHook() {
    override val key = "quick_manage_unknown_app_sources"
    override fun hook() {
        loadClass("com.android.settings.SettingsActivity").methodFinder().filterByName("redirectTabletActivity").first().createHook {
            before {
                val intent = (it.thisObject as Activity).intent
                if (intent.action != Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES || intent.data == null || intent.data!!.scheme != "package") return@before
                it.thisObject.objectHelper().setObjectUntilSuperclass(
                    "initialFragmentName", "com.android.settings.applications.appinfo.ExternalSourcesDetails"
                )
            }
        }
    }
}