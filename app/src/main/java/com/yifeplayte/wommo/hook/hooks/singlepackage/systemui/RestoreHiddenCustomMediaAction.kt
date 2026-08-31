package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object RestoreHiddenCustomMediaAction : BaseHook() {
    override val key = "restore_hidden_custom_media_action"
    private val notificationSettingsManager by lazy {
        val clazzDependency = loadClass("com.android.systemui.Dependency")
        val clazzNotificationSettingsManager =
            loadClass("com.android.systemui.statusbar.notification.NotificationSettingsManager")
        clazzDependency.callStaticMethod("get", clazzNotificationSettingsManager)
    }

    override fun hook() {
        loadClass("com.android.systemui.media.controls.pipeline.MediaDataManager").findMethod {
            name("createActionsFromState")
        }.createHook {
            before {
                (notificationSettingsManager ?: return@before).putField(
                    "mHiddenCustomActionsList",
                    listOf<String>()
                )
            }
        }
    }
}