package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.widget.ImageView
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethodOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.getStaticFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_HYPER_OS

@Suppress("unused")
object RedirectToNotificationChannelSetting : BaseHook() {
    override val key: String = "redirect_to_notification_channel_setting"
    override fun hook() {
        var statusBarNotification: StatusBarNotification? = null
        val clazzMiuiNotificationMenuRow =
            loadClass("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow")
        if (IS_HYPER_OS) {
            val clazzDependency = loadClass("com.android.systemui.Dependency")
            val clazzModalController =
                loadClass("com.android.systemui.statusbar.notification.modal.ModalController")
            val clazzCommandQueue = loadClass("com.android.systemui.statusbar.CommandQueue")
            clazzMiuiNotificationMenuRow.findMethod { name("createMenuViews") }.createHook {
                after { param ->
                    val mSbn =
                        param.thisObject.getFieldOrNullAs<StatusBarNotification>("mSbn")
                            ?: return@after
                    val mInfoItem =
                        param.thisObject.getFieldOrNull("mInfoItem") ?: return@after
                    EzXposed.initAppContext(param.thisObject.getFieldOrNullAs<Context>("mContext"))
                    val mIcon = (mInfoItem as Any).getFieldOrNullAs<ImageView>("mIcon") ?: return@after
                    mIcon.setOnClickListener {
                        startChannelNotificationSettings(mSbn)
                        val modalController = clazzDependency.callStaticMethod(
                            "get", clazzModalController
                        ) ?: return@setOnClickListener
                        modalController.callMethod(
                            "animExitModal", 50L, true, "MORE", false
                        )
                        val commandQueue = clazzDependency.callStaticMethod(
                            "get", clazzCommandQueue
                        ) ?: return@setOnClickListener
                        commandQueue.callMethod(
                            "animateCollapsePanels", 0, false
                        )
                    }
                }
            }
        }
        clazzMiuiNotificationMenuRow.findMethodOrNull { name("onClickInfoItem") }
            ?.createHook {
                before { param ->
                    val self = param.thisObject
                    EzXposed.initAppContext(self.getFieldOrNullAs("mContext"))
                    statusBarNotification = self.getFieldOrNullAs("mSbn")
                }
                after {
                    statusBarNotification = null
                }
            }
        loadClass("com.android.systemui.statusbar.notification.NotificationSettingsHelper").findMethodOrNull {
            name("startAppNotificationSettings")
        }?.createHook {
                before { param ->
                    startChannelNotificationSettings(statusBarNotification!!)
                    param.result = null
                }
            }
    }

    private fun startChannelNotificationSettings(statusBarNotification: StatusBarNotification) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setClassName(
                "com.android.settings", "com.android.settings.SubSettings"
            )
            putExtra(
                ":android:show_fragment",
                "com.android.settings.notification.ChannelNotificationSettings"
            )
            putExtra(
                Settings.EXTRA_APP_PACKAGE, statusBarNotification.packageName
            )
            putExtra(
                Settings.EXTRA_CHANNEL_ID, statusBarNotification.notification.channelId
            )
            putExtra("app_uid", statusBarNotification.uid).putExtra(
                Settings.EXTRA_CONVERSATION_ID, statusBarNotification.notification.shortcutId
            )
        }
        val userHandleCurrent = UserHandle::class.java.getStaticFieldOrNullAs<UserHandle>("CURRENT")
        EzXposed.appContext.callMethod("startActivityAsUser", intent, userHandleCurrent)
    }
}
