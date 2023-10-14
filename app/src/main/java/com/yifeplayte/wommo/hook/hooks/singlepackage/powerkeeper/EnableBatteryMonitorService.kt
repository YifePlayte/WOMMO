package com.yifeplayte.wommo.hook.hooks.singlepackage.powerkeeper

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

object EnableBatteryMonitorService : BaseHook() {
    override val key = "enable_battery_monitor_service"
    override fun hook() {
        loadClass("com.miui.powerkeeper.utils.Utils").methodFinder().filterByName("isDevelopmentOrDebugVersion").first().createHook {
            returnConstant(true)
        }
    }
}