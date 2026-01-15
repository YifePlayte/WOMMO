package com.yifeplayte.wommo.hook.hooks.singlepackage.aiengine

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object ChangeBrowserForAIEngine : BaseHook() {
    override val key = "change_browser_for_ai_engine"
    override fun hook() {
        loadClass("com.xiaomi.aicr.copydirect.util.SmartPasswordUtils")
            .methodFinder().filterByName("jumpToXiaoMiBrowser").single().createHook {
                replace { param ->
                    Intent(Intent.ACTION_VIEW, param.args[1].toString().withHttpsIfMissing().toUri()).let {
                        (param.args[0] as Context).startActivity(it)
                    }
                }
            }
    }

    fun String.withHttpsIfMissing(): String = if (startsWith("http://", true) || startsWith("https://", true)) this else "https://$this"
}