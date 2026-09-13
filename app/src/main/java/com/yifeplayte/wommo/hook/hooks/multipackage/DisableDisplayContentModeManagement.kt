package com.yifeplayte.wommo.hook.hooks.multipackage

import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

@Suppress("unused")
object DisableDisplayContentModeManagement : BaseMultiHook() {
    override val key = "disable_display_content_mode_management"
    override val hooks = mapOf(
        "system" to { hook() },
        "com.android.systemui" to { hook() },
    )

    private fun hook() {
        val clazzDisplayFeatureFlag = loadClass("com.android.internal.hidden_from_bootclasspath.com.android.server.display.feature.flags.Flags")
        clazzDisplayFeatureFlag.findMethod {
            name("enableDisplayContentModeManagement")
        }.createHook {
            returnConstant(false)
        }
        val clazzWindowFlags = loadClass("com.android.internal.hidden_from_bootclasspath.com.android.window.flags.Flags")
        clazzWindowFlags.findMethod {
            name("enableSysDecorsCallbacksViaWm")
        }.createHook {
            returnConstant(false)
        }
    }
}