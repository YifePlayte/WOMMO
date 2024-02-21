package com.yifeplayte.wommo.hook.hooks.multipackage

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObjectUntilSuperclass
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField
import de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField

@Suppress("unused")
object ExposureRefreshForNonMIUIWidget : BaseMultiHook() {
    override val key = "exposure_refresh_for_non_miui_widget"
    override val hooks = mapOf(
        "com.miui.personalassistant" to { personalAssistant() },
        "android" to { android() },
    )

    private fun personalAssistant() {
        val clazzAppWidgetItemInfo =
            loadClass("com.miui.personalassistant.widget.iteminfo.AppWidgetItemInfo")
        clazzAppWidgetItemInfo.methodFinder().filterByName("parseWidgetMetaData").single()
            .createHook {
                after {
                    val receiverInfo = appContext.packageManager.getReceiverInfo(
                        getObjectOrNull(it.thisObject, "provider") as ComponentName,
                        PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                    )
                    val isRealMIUIWidget = receiverInfo.metaData.getBoolean("miuiWidget")
                    setAdditionalInstanceField(
                        it.thisObject, "isRealMIUIWidget", isRealMIUIWidget
                    )
                    if (!isRealMIUIWidget) {
                        setObjectUntilSuperclass(it.thisObject, "isMIUIWidget", true)
                        setObject(it.thisObject, "miuiWidgetRefresh", "exposure")
                        setObject(it.thisObject, "miuiWidgetRefreshMinInterval", 10000)
                    }
                }
            }
        clazzAppWidgetItemInfo.methodFinder().filterByName("obtainMiuiWidgetUpdateIntent").single()
            .createHook {
                after { param ->
                    if (!(getAdditionalInstanceField(
                            param.thisObject, "isRealMIUIWidget"
                        ) as Boolean)
                    ) {
                        (param.result as Intent).action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    }
                    appContext.sendBroadcast(param.result as Intent)
                }
            }
    }

    private fun android() {
        // try to bypass the permission for third-party apps to send update broadcast to other apps
        loadClass("com.android.server.am.ActivityManagerService").methodFinder()
            .filterByName("broadcastIntentLocked").toList().createHooks {
                before {
                    val intent = it.args[3] as Intent
                    if (intent.action in setOf(
                            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                            AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                        )
                    ) {
                        it.args[1] = intent.component?.packageName
                    }
                }
            }
        loadClassOrNull("com.android.server.appwidget.AppWidgetServiceImplStubImpl")?.methodFinder()
            ?.filterByName("isForMiui")?.single()?.createHook {
                returnConstant(false)
            }
    }
}