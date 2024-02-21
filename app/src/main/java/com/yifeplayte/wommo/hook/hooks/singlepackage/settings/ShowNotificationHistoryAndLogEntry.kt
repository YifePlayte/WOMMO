package com.yifeplayte.wommo.hook.hooks.singlepackage.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.util.AttributeSet
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.HookFactory
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object ShowNotificationHistoryAndLogEntry : BaseHook() {
    override val key = "show_notification_history_and_log_entry"
    override fun hook() {
        val hook: HookFactory.() -> Unit = {
            after {
                val thisObject = it.thisObject
                val resources = invokeMethodBestMatch(thisObject, "getResources") as Resources
                val preferenceManager = invokeMethodBestMatch(thisObject, "getPreferenceManager")!!
                val context = invokeMethodBestMatch(preferenceManager, "getContext") as Context
                invokeMethodBestMatch(
                    thisObject, "findPreference", null, "notification_settings"
                )?.objectHelper {
                    invokeMethodBestMatch(
                        "addPreference", null, generatePreferenceScreen(
                            context,
                            resources,
                            "com.android.settings.notification.history.NotificationHistoryActivity",
                            "notification_history_title"
                        )
                    )
                    invokeMethodBestMatch(
                        "addPreference", null, generatePreferenceScreen(
                            context,
                            resources,
                            "com.android.settings.Settings\$NotificationStationActivity",
                            "notification_log_title"
                        )

                    )
                }
            }
        }
        runCatching {
            loadClass("com.android.settings.NotificationControlCenterSettings").methodFinder()
                .filterByName("onCreate").single().createHook(hook)
        }
        runCatching {
            loadClass("com.android.settings.NotificationStatusBarSettings").methodFinder()
                .filterByName("onCreate").single().createHook(hook)
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun generatePreferenceScreen(
        context: Context, resources: Resources, className: String, titleIdName: String
    ): Any {
        val preferenceScreenForNotificationHistory =
            loadClass("androidx.preference.PreferenceScreen").getDeclaredConstructor(
                Context::class.java, AttributeSet::class.java
            ).newInstance(context, null)
        invokeMethodBestMatch(
            preferenceScreenForNotificationHistory,
            "setIntent",
            null,
            Intent().apply {
                action = Intent.ACTION_MAIN
                setClassName(
                    "com.android.settings", className
                )
            })
        invokeMethodBestMatch(
            preferenceScreenForNotificationHistory,
            "setTitle",
            null,
            resources.getIdentifier(titleIdName, "string", hostPackageName)
        )
        return preferenceScreenForNotificationHistory
    }
}