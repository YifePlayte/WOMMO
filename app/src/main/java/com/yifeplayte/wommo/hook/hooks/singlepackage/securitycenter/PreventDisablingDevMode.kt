package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object PreventDisablingDevMode : BaseHook() {
    override val key = "prevent_disabling_dev_mode"
    override fun hook() {
        loadClass("com.miui.securityscan.model.system.DevModeModel").methodFinder()
            .filterByName("optimize").single().createHook {
                returnConstant(null)
            }
        loadClass("com.miui.securityscan.model.system.UsbModel").methodFinder()
            .filterByName("optimize").single().createHook {
                returnConstant(null)
            }
    }
}