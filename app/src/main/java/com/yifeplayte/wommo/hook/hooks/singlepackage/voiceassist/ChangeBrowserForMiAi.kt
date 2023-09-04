package com.yifeplayte.wommo.hook.hooks.singlepackage.voiceassist

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

object ChangeBrowserForMiAi : BaseHook() {
    override val key = "change_browser_for_mi_ai"
    override fun hook() {
        dexKitBridge.findMethod {
            methodName = "parseIntentData"
            methodParamTypes = arrayOf("Lcom/xiaomi/ai/api/Template\$AndroidIntent;", "", "")
        }.map { it.getMethodInstance(safeClassLoader) }.createHooks {
            before {
                if (invokeMethodBestMatch(it.args[0], "getPkgName")?.equals("com.android.browser") == true) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(invokeMethodBestMatch(it.args[0], "getUri") as String?))
                    @SuppressLint("QueryPermissionsNeeded") val packageName: String =
                        intent.resolveActivityInfo(appContext.packageManager, 0).packageName
                    invokeMethodBestMatch(it.args[0], "setPkgName", null, packageName)
                }
            }
        }
    }
}