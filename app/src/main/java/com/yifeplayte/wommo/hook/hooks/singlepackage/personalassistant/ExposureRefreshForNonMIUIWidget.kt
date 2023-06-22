package com.yifeplayte.wommo.hook.hooks.singlepackage.personalassistant

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObjectUntilSuperclass
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField
import de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField

object ExposureRefreshForNonMIUIWidget : BaseHook() {
    override val key = "exposure_refresh_for_non_miui_widget"
    override fun hook() {
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
                setAdditionalInstanceField(it.thisObject, "isRealMIUIWidget", isRealMIUIWidget)
                if (!isRealMIUIWidget) {
                    setObjectUntilSuperclass(it.thisObject, "isMIUIWidget", true)
                    setObject(it.thisObject, "miuiWidgetRefresh", "exposure")
                    setObject(it.thisObject, "miuiWidgetRefreshMinInterval", 10000)
                }
            }
        }
        clazzAppWidgetItemInfo.methodFinder().filterByName("obtainMiuiWidgetUpdateIntent").first().createHook {
            after {
                if (!(getAdditionalInstanceField(it.thisObject, "isRealMIUIWidget") as Boolean)) {
                    (it.result as Intent).action = "android.appwidget.action.APPWIDGET_UPDATE"
                }
            }
        }
    }
}