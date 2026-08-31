package com.yifeplayte.wommo.hook.hooks.singlepackage.settings

import android.app.Activity
import android.provider.Settings
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object QuickManageOverlayPermission : BaseHook() {
    override val key = "quick_manage_overlay_permission"
    override fun hook() {
        loadClass("com.android.settings.SettingsActivity").findMethod {
            name("redirectTabletActivity")
        }.createHook {
            before {
                val activity = it.thisObject as Activity
                val intent = activity.intent
                val action = intent.action
                val data = intent.data
                if (action != Settings.ACTION_MANAGE_OVERLAY_PERMISSION || data == null || data.scheme != "package") return@before
                activity.putField(
                    "initialFragmentName",
                    "com.android.settings.applications.appinfo.DrawOverlayDetails"
                )
            }
        }
    }
}