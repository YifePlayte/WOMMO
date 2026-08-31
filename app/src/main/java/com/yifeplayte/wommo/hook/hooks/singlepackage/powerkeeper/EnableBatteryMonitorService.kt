package com.yifeplayte.wommo.hook.hooks.singlepackage.powerkeeper

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object EnableBatteryMonitorService : BaseHook() {
    override val key = "enable_battery_monitor_service"
    override fun hook() {
        loadClass("com.miui.powerkeeper.utils.Utils").findMethod {
            name("isDevelopmentOrDebugVersion")
        }.createHook {
            returnConstant(true)
        }
    }
}