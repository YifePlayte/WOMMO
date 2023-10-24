package com.yifeplayte.wommo.hook.hooks.singlepackage.contentextension

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

object ChangeBrowserForContentExtension : BaseHook() {
    override val key = "change_browser_for_content_extension"
    override fun hook() {
        val clazzAppsUtils = loadClass("com.miui.contentextension.utils.AppsUtils")
        clazzAppsUtils.methodFinder().filterByName("openGlobalSearch").first().createHook {
            replace { param ->
                Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, param.args[1].toString())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }.let {
                    appContext.startActivity(it)
                }
            }
        }
        clazzAppsUtils.methodFinder().filterByName("getIntentWithBrowser").first().createHook {
            before {
                it.result = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(it.args[0].toString())
                }
            }
        }
    }
}