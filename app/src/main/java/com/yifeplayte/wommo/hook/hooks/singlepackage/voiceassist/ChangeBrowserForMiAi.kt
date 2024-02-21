package com.yifeplayte.wommo.hook.hooks.singlepackage.voiceassist

import android.content.Intent
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

@Suppress("unused")
object ChangeBrowserForMiAi : BaseHook() {
    override val key = "change_browser_for_mi_ai"
    override fun hook() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("addBackForUri intent is null")
                returnType = "android.content.Intent"
            }
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