package com.yifeplayte.wommo.hook.hooks.multipackage

import android.app.NotificationChannel
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullUntilSuperclass
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullUntilSuperclassAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField
import de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField

@Suppress("unused")
object ShowNotificationImportance : BaseMultiHook() {
    override val key = "show_notification_importance"
    override val hooks = mapOf(
        "com.android.settings" to { settings() },
        "com.android.systemui" to { systemUi() },
    )

    private fun settings() {
        loadClass("com.android.settings.notification.ChannelNotificationSettings").methodFinder()
            .filterByName("removeDefaultPrefs").single().createHook {
                before {
                    val importance =
                        invokeMethodBestMatch(it.thisObject, "findPreference", null, "importance")
                            ?: return@before
                    val mChannel = getObjectOrNullUntilSuperclassAs<NotificationChannel>(
                        it.thisObject,
                        "mChannel"
                    )!!
                    val index = invokeMethodBestMatch(
                        importance, "findSpinnerIndexOfValue", null, mChannel.importance.toString()
                    )!! as Int
                    if (index < 0) return@before
                    invokeMethodBestMatch(importance, "setValueIndex", null, index)
                    setAdditionalInstanceField(
                        importance,
                        "channelNotificationSettings",
                        it.thisObject
                    )
                    it.result = null
                }
            }
        loadClass("androidx.preference.Preference").methodFinder()
            .filterByName("callChangeListener")
            .filterByParamTypes(Any::class.java).single().createHook {
                after {
                    val channelNotificationSettings =
                        getAdditionalInstanceField(it.thisObject, "channelNotificationSettings")
                            ?: return@after
                    val mChannel =
                        getObjectOrNullUntilSuperclassAs<NotificationChannel>(
                            channelNotificationSettings,
                            "mChannel"
                        )!!
                    invokeMethodBestMatch(
                        mChannel,
                        "setImportance",
                        null,
                        (it.args[0] as String).toInt()
                    )
                    val mBackend =
                        getObjectOrNullUntilSuperclass(channelNotificationSettings, "mBackend")!!
                    val mPkg = getObjectOrNullUntilSuperclassAs<String>(
                        channelNotificationSettings,
                        "mPkg"
                    )
                    val mUid =
                        getObjectOrNullUntilSuperclassAs<Int>(channelNotificationSettings, "mUid")
                    invokeMethodBestMatch(mBackend, "updateChannel", null, mPkg, mUid, mChannel)
                }
            }
    }

    private fun systemUi() {
        loadClass("com.android.systemui.statusbar.phone.NotificationIconAreaController").methodFinder()
            .filterByName("updateStatusBarIcons").single().createHook {
                before { param ->
                    val mNotificationEntries = getObjectOrNullAs<List<Any>>(
                        param.thisObject,
                        "mNotificationEntries"
                    )!!
                    if (mNotificationEntries.isNotEmpty()) {
                        val list = ArrayList<Any>()
                        mNotificationEntries.forEach {
                            val representativeEntry =
                                invokeMethodBestMatch(it, "getRepresentativeEntry")!!
                            val importance =
                                invokeMethodBestMatch(representativeEntry, "getImportance") as Int
                            if (importance > 1) list.add(it)
                        }
                        if (list.size != mNotificationEntries.size) {
                            setObject(param.thisObject, "mNotificationEntries", list)
                        }
                    }
                }
            }
    }
}