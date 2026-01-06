package com.yifeplayte.wommo.hook.hooks.subpackage.systemuiplugin

import android.app.Notification
import android.app.PendingIntent
import android.os.Bundle
import android.os.UserHandle
import android.service.notification.StatusBarNotification
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseSubHook
import java.lang.System.currentTimeMillis

@Suppress("unused")
object CanIslandSlide : BaseSubHook() {
    override val key = "can_island_slide"
    override val isEnabled = true
    override fun hook(subClassLoader: ClassLoader) {
        val clazzDynamicIslandBaseContentView = loadClass(
            "miui.systemui.dynamicisland.window.content.DynamicIslandBaseContentView",
            subClassLoader
        )
        clazzDynamicIslandBaseContentView.methodFinder()
            .filterByName("canExpandedViewSlide")
            .single()
            .createHook {
                before {
                    // Log.i("canExpandedViewSlide:")
                    val dynamicIslandContentView = it.args[0]
                    // Log.i("dynamicIslandContentView: $dynamicIslandContentView")
                    val currentIslandData = invokeMethodBestMatch(dynamicIslandContentView, "getCurrentIslandData") ?: return@before
                    val extras = invokeMethodBestMatch(currentIslandData, "getExtras") as? Bundle ?: return@before
                    val pendingIntent = extras.getParcelable("miui.pending.intent", PendingIntent::class.java) ?: return@before
                    Log.i("miui.pending.intent: $pendingIntent")

                    var thisCurrentIslandData = getObjectOrNull(it.thisObject, "currentIslandData", clazzDynamicIslandBaseContentView)
                    if (thisCurrentIslandData == null) {
                        setObject(it.thisObject, "currentIslandData", currentIslandData, clazzDynamicIslandBaseContentView)
                        thisCurrentIslandData = currentIslandData
                        Log.i("thisCurrentIslandData is null, replaced")
                    }

                    Log.i("thisCurrentIslandData: $thisCurrentIslandData")
                    val thisExtras = invokeMethodBestMatch(thisCurrentIslandData, "getExtras") as? Bundle ?: return@before
                    Log.i("thisExtras: $thisExtras")
                    var statusBarNotification = thisExtras.getParcelable("miui.sbn", StatusBarNotification::class.java)
                    if (statusBarNotification == null) {
                        statusBarNotification = StatusBarNotification(
                            "com.android.systemui",
                            "com.android.systemui",
                            1,
                            "WOMMO",
                            10000,
                            0,
                            0,
                            Notification().apply { contentIntent = pendingIntent },
                            UserHandle.getUserHandleForUid(0),
                            currentTimeMillis()
                        )
                        thisExtras.putParcelable("miui.sbn", statusBarNotification)
                        Log.i("miui.sbn is null, created")
                    }

                    Log.i("statusBarNotification: $statusBarNotification")
                    val contentIntent = statusBarNotification.notification?.contentIntent ?: return@before
                    Log.i("contentIntent: $contentIntent")

                    statusBarNotification.notification?.contentIntent = pendingIntent
                    extras.putParcelable("miui.pending.intent", null)
                }
            }
    }
}