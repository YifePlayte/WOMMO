package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.view.View
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.EzXHelper.moduleRes
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.loadDexKit
import io.luckypray.dexkit.enums.MatchType

object OpenByDefaultSetting : BaseHook() {
    override val key = "open_by_default_setting"

    @SuppressLint("DiscouragedApi")
    override fun hook() {
        val domainVerificationManager: DomainVerificationManager by lazy {
            appContext.getSystemService(
                DomainVerificationManager::class.java
            )
        }
        loadClass("com.miui.appmanager.ApplicationsDetailsActivity").methodFinder().first {
            name == "onClick"
        }.createHook {
            before { param ->
                EzXHelper.initAppContext(param.thisObject as Activity)
                val clickedView = param.args[0]
                val cleanOpenByDefaultView = (param.thisObject as Activity).findViewById<View>(
                    appContext.resources.getIdentifier(
                        "am_detail_default", "id", hostPackageName
                    )
                )
                val pkgName = (param.thisObject as Activity).intent.getStringExtra("package_name")!!
                if (clickedView == cleanOpenByDefaultView) {
                    val intent = Intent().apply {
                        action = android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
                        addCategory(Intent.CATEGORY_DEFAULT)
                        data = android.net.Uri.parse("package:${pkgName}")
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    }
                    invokeMethodBestMatch(param.thisObject, "startActivity", null, intent)
                    param.result = null
                }
            }
        }

        loadDexKit()
        dexKitBridge.findMethodUsingString {
            usingString = "enter_way"
            matchType = MatchType.CONTAINS
            methodDeclareClass = "Lcom/miui/appmanager/ApplicationsDetailsActivity;"
            methodReturnType = "void"
        }.firstOrNull()?.getMethodInstance(EzXHelper.safeClassLoader)?.createHook {
            after { param ->
                EzXHelper.initAppContext(param.thisObject as Activity)
                val cleanOpenByDefaultView = (param.thisObject as Activity).findViewById<View>(
                    appContext.resources.getIdentifier(
                        "am_detail_default", "id", hostPackageName
                    )
                )
                val pkgName = (param.thisObject as Activity).intent.getStringExtra("package_name")!!
                val isLinkHandlingAllowed = domainVerificationManager.getDomainVerificationUserState(
                    pkgName
                )?.isLinkHandlingAllowed ?: false
                val subTextId =
                    if (isLinkHandlingAllowed) R.string.app_link_open_always else R.string.app_link_open_never
                cleanOpenByDefaultView::class.java.declaredFields.forEach {
                    val view = getObjectOrNull(cleanOpenByDefaultView, it.name)
                    if (view !is TextView) return@forEach
                    invokeMethodBestMatch(
                        view, "setText", null, moduleRes.getString(R.string.open_by_default)
                    )
                }
                invokeMethodBestMatch(
                    cleanOpenByDefaultView, "setSummary", null, moduleRes.getString(subTextId)
                )
            }
        }
    }
}