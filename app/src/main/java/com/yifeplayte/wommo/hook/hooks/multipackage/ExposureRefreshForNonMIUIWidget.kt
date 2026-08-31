package com.yifeplayte.wommo.hook.hooks.multipackage

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import com.yifeplayte.wommo.hook.utils.AdditionalFields
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethodOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

@Suppress("unused")
object ExposureRefreshForNonMIUIWidget : BaseMultiHook() {
    override val key = "exposure_refresh_for_non_miui_widget"
    override val hooks = mapOf(
        "com.miui.personalassistant" to { personalAssistant() },
        "system" to { system() },
    )

    private fun personalAssistant() {
        val clazzAppWidgetItemInfo =
            loadClass("com.miui.personalassistant.widget.iteminfo.AppWidgetItemInfo")
        clazzAppWidgetItemInfo.findMethod { name("parseWidgetMetaData") }.createHook {
            after {
                val receiverInfo = EzXposed.appContext.packageManager.getReceiverInfo(
                    it.thisObject.getFieldOrNull("provider") as ComponentName,
                    PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
                val isRealMIUIWidget = receiverInfo.metaData.getBoolean("miuiWidget")
                AdditionalFields.set(it.thisObject, "isRealMIUIWidget", isRealMIUIWidget)
                if (!isRealMIUIWidget) {
                    it.thisObject.putField("isMIUIWidget", true)
                    it.thisObject.putField("miuiWidgetRefresh", "exposure")
                    it.thisObject.putField("miuiWidgetRefreshMinInterval", 10000)
                }
            }
        }
        clazzAppWidgetItemInfo.findMethod { name("obtainMiuiWidgetUpdateIntent") }.createHook {
            after { param ->
                if (!(AdditionalFields.get(
                        param.thisObject, "isRealMIUIWidget"
                    ) as Boolean)
                ) {
                    (param.result as Intent).action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
                EzXposed.appContext.sendBroadcast(param.result as Intent)
            }
        }
    }

    private fun system() {
        // try to bypass the permission for third-party apps to send update broadcast to other apps
        loadClass("com.android.server.am.ActivityManagerService").findAllMethods {
            name("broadcastIntentLocked")
        }.createHooks {
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
        loadClassOrNull("com.android.server.appwidget.AppWidgetServiceImplStubImpl")?.findMethodOrNull {
            name("isForMiui")
        }?.createHook {
            returnConstant(false)
        }
    }
}
