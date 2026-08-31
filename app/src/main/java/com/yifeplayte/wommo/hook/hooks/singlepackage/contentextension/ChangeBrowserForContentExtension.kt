package com.yifeplayte.wommo.hook.hooks.singlepackage.contentextension

import android.app.SearchManager
import android.content.Intent
import androidx.core.net.toUri
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object ChangeBrowserForContentExtension : BaseHook() {
    override val key = "change_browser_for_content_extension"
    override fun hook() {
        val clazzAppsUtils = loadClass("com.miui.contentextension.utils.AppsUtils")
        clazzAppsUtils.findMethod { name("openGlobalSearch") }.createHook {
            replace { param ->
                Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, param.args[1].toString())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }.let {
                    EzXposed.appContext.startActivity(it)
                }
            }
        }
        clazzAppsUtils.findMethod { name("getIntentWithBrowser") }.createHook {
            before {
                it.result = Intent(Intent.ACTION_VIEW, it.args[0].toString().toUri())
            }
        }
    }
}