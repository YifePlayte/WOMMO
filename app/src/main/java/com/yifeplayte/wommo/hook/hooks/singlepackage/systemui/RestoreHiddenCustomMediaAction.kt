package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

object RestoreHiddenCustomMediaAction : BaseHook() {
    override val key = "restore_hidden_custom_media_action"
    private val notificationSettingsManager by lazy {
        val clazzDependency = loadClass("com.android.systemui.Dependency")
        val clazzNotificationSettingsManager =
            loadClass("com.android.systemui.statusbar.notification.NotificationSettingsManager")
        invokeStaticMethodBestMatch(
            clazzDependency, "get", null, clazzNotificationSettingsManager
        )
    }

    override fun hook() {
        loadClass("com.android.systemui.media.controls.pipeline.MediaDataManager").methodFinder()
            .filterByName("createActionsFromState").first().createHook {
                before {
                    setObject(
                        notificationSettingsManager ?: return@before,
                        "mHiddenCustomActionsList",
                        listOf<String>()
                    )
                }
            }
    }
}