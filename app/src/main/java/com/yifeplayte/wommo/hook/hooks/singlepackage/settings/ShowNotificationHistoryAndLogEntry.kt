package com.yifeplayte.wommo.hook.hooks.singlepackage.settings

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.util.AttributeSet
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.hostPackageName
import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.HookFactory
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

@Suppress("unused")
object ShowNotificationHistoryAndLogEntry : BaseHook() {
    override val key = "show_notification_history_and_log_entry"
    override fun hook() {
        val hook: HookFactory.() -> Unit = {
            after {
                val thisObject = it.thisObject
                val resources = thisObject.callMethod("getResources") as Resources
                val preferenceManager = thisObject.callMethod("getPreferenceManager")!!
                val context = preferenceManager.callMethod("getContext") as Context
                val parent = thisObject.callMethod(
                    "findPreference", "notification_managing"
                )?.callMethod("getParent")!!
                parent.callMethod(
                    "addPreference", generatePreferenceScreen(
                        context,
                        resources,
                        "com.android.settings.notification.history.NotificationHistoryActivity",
                        "notification_history_title"
                    )
                )
                parent.callMethod(
                    "addPreference", generatePreferenceScreen(
                        context,
                        resources,
                        $$"com.android.settings.Settings$NotificationStationActivity",
                        "notification_log_title"
                    )
                )
            }
        }
        runCatching {
            loadClass("com.android.settings.NotificationControlCenterSettings").findMethod {
                name("onCreate")
            }.createHook(priority = XposedInterface.PRIORITY_DEFAULT, block = hook)
        }
        runCatching {
            loadClass("com.android.settings.NotificationStatusBarSettings").findMethod {
                name("onCreate")
            }.createHook(priority = XposedInterface.PRIORITY_DEFAULT, block = hook)
        }
    }

    @android.annotation.SuppressLint("DiscouragedApi")
    private fun generatePreferenceScreen(
        context: Context, resources: Resources, className: String, titleIdName: String
    ): Any {
        val preferenceScreenForNotificationHistory =
            loadClass("androidx.preference.Preference").getDeclaredConstructor(
                Context::class.java, AttributeSet::class.java
            ).newInstance(context, null)
        preferenceScreenForNotificationHistory.callMethod(
            "setIntent",
            Intent().apply {
                action = Intent.ACTION_MAIN
                setClassName(
                    "com.android.settings", className
                )
            })
        preferenceScreenForNotificationHistory.callMethod(
            "setTitle",
            resources.getIdentifier(titleIdName, "string", hostPackageName)
        )
        return preferenceScreenForNotificationHistory
    }
}
