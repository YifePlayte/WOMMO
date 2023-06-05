package com.yifeplayte.wommo.hook.hooks.personalassistant

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObjectUntilSuperclass
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import de.robv.android.xposed.XposedHelpers

object ExposureRefreshForNonMIUIWidget : BaseHook() {
    override fun init() {
        val clazzAppWidgetItemInfo = loadClass("com.miui.personalassistant.widget.iteminfo.AppWidgetItemInfo")
        clazzAppWidgetItemInfo.methodFinder().filterByName("parseWidgetMetaData").first().createHook {
            after {
                EzXHelper.initAppContext()
                val receiverInfo = EzXHelper.appContext.packageManager.getReceiverInfo(
                    getObjectOrNull(
                        it.thisObject, "provider"
                    ) as ComponentName, PackageManager.GET_META_DATA
                )
                val isRealMIUIWidget = receiverInfo.metaData.getBoolean("miuiWidget")
                XposedHelpers.setAdditionalInstanceField(it.thisObject, "isRealMIUIWidget", isRealMIUIWidget)
                if (!isRealMIUIWidget) {
                    XposedHelpers.setBooleanField(it.thisObject, "isMIUIWidget", true)
                    // setObjectUntilSuperclass(it.thisObject, "isMIUIWidget", true)
                    setObject(it.thisObject, "miuiWidgetRefresh", "exposure")
                    setObject(it.thisObject, "miuiWidgetRefreshMinInterval", 10000)
                }
            }
        }
        clazzAppWidgetItemInfo.methodFinder().filterByName("obtainMiuiWidgetUpdateIntent").first().createHook {
            after {
                if (!(XposedHelpers.getAdditionalInstanceField(it.thisObject, "isRealMIUIWidget") as Boolean)) {
                    (it.result as Intent).action = "android.appwidget.action.APPWIDGET_UPDATE"
                }
            }
        }
    }
}