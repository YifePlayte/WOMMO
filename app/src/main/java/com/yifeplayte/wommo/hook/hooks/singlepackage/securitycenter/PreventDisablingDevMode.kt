package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object PreventDisablingDevMode : BaseHook() {
    override val key = "prevent_disabling_dev_mode"
    override fun hook() {
        loadClass("com.miui.securityscan.model.system.DevModeModel").findMethod {
            name("optimize")
        }.createHook {
            returnConstant(null)
        }
        loadClass("com.miui.securityscan.model.system.UsbModel").findMethod {
            name("optimize")
        }.createHook {
            returnConstant(null)
        }
    }
}