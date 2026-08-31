package com.yifeplayte.wommo.hook.hooks.multipackage

import android.app.NotificationChannel
import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import com.yifeplayte.wommo.hook.utils.AdditionalFields
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

@Suppress("unused")
object ShowNotificationImportance : BaseMultiHook() {
    override val key = "show_notification_importance"
    override val hooks = mapOf(
        "com.android.settings" to { settings() },
        "com.android.systemui" to { systemUi() },
    )

    private fun settings() {
        loadClass("com.android.settings.notification.ChannelNotificationSettings").findMethod {
            name("removeDefaultPrefs")
        }.createHook {
            before {
                val importance =
                    it.thisObject.callMethod("findPreference", "importance")
                        ?: return@before
                val mChannel = it.thisObject.getFieldOrNullAs<NotificationChannel>(
                    "mChannel"
                )!!
                val index = importance.callMethod(
                    "findSpinnerIndexOfValue", mChannel.importance.toString()
                )!! as Int
                if (index < 0) return@before
                importance.callMethod("setValueIndex", index)
                AdditionalFields.set(
                    importance,
                    "channelNotificationSettings",
                    it.thisObject
                )
                it.result = null
            }
        }
        loadClass("androidx.preference.Preference").findMethod {
            name("callChangeListener")
            params(Any::class.java)
        }.createHook {
            after {
                val channelNotificationSettings =
                    AdditionalFields.get(it.thisObject, "channelNotificationSettings")
                        ?: return@after
                val mChannel =
                    channelNotificationSettings.getFieldOrNullAs<NotificationChannel>(
                        "mChannel"
                    )!!
                mChannel.callMethod(
                    "setImportance",
                    (it.args[0] as String).toInt()
                )
                val mBackend =
                    channelNotificationSettings.getFieldOrNull("mBackend")!!
                val mPkg = channelNotificationSettings.getFieldOrNullAs<String>(
                    "mPkg"
                )
                val mUid =
                    channelNotificationSettings.getFieldOrNullAs<Int>("mUid")
                mBackend.callMethod("updateChannel", mPkg, mUid, mChannel)
            }
        }
    }

    private fun systemUi() {
        loadClass("com.android.systemui.statusbar.phone.NotificationIconAreaController").findMethod {
            name("updateStatusBarIcons")
        }.createHook {
            before { param ->
                val mNotificationEntries = param.thisObject.getFieldOrNullAs<List<Any>>(
                    "mNotificationEntries"
                )!!
                if (mNotificationEntries.isNotEmpty()) {
                    val list = ArrayList<Any>()
                    mNotificationEntries.forEach {
                        val representativeEntry =
                            it.callMethod("getRepresentativeEntry")!!
                        val importance =
                            representativeEntry.callMethod("getImportance") as Int
                        if (importance > 1) list.add(it)
                    }
                    if (list.size != mNotificationEntries.size) {
                        param.thisObject.putField("mNotificationEntries", list)
                    }
                }
            }
        }
    }
}
