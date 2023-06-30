package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

object UnlockControlCenterStyle : BaseHook() {
    override val key = "unlock_control_center_style"
    override val isEnabled = true
    override fun hook() {
        loadClass("com.android.systemui.controlcenter.policy.ControlCenterControllerImpl").declaredConstructors.createHooks {
            after {
                setObject(it.thisObject, "forceUseControlCenterPanel", false)
            }
        }
        loadClass("com.miui.systemui.SettingsObserver").methodFinder().filterByName("setValue\$default").first()
            .createHook {
                before {
                    if (it.args[1] == "force_use_control_panel") {
                        it.args[2] = 0
                    }
                }
            }
    }
}