package com.yifeplayte.wommo.hook.hooks.singlepackage.voiceassist

import android.content.Intent
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

object ChangeBrowserForMiAi : BaseHook() {
    override val key = "change_browser_for_mi_ai"
    override fun hook() {
        dexKitBridge.findMethodUsingString {
            usingString = "addBackForUri intent is null"
            methodReturnType = "Landroid/content/Intent;"
        }.map { it.getMethodInstance(safeClassLoader) }.createHooks {
            after {
                val intent = it.result as Intent
                if (intent.`package`.equals("com.android.browser")) {
                    it.result = Intent(Intent.ACTION_VIEW, intent.data)
                }
            }
        }
    }
}