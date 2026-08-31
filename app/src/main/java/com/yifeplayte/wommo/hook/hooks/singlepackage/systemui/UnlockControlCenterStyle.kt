package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object UnlockControlCenterStyle : BaseHook() {
    override val key = "unlock_control_center_style"
    override fun hook() {
        loadClass("com.android.systemui.controlcenter.policy.ControlCenterControllerImpl").declaredConstructors.toList().createHooks {
            after {
                it.thisObject.putField("forceUseControlCenterPanel", false)
            }
        }
        loadClass("com.miui.systemui.SettingsObserver").findMethod {
            name($$"setValue\$default")
        }.createHook {
            before {
                if (it.args[1] == "force_use_control_panel") {
                    it.args[2] = 0
                }
            }
        }
    }
}