package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object DisableNetworkAssistantOfflineInfoManager : BaseHook() {
    override val key = "disable_network_assistant_offline_info_manager"
    override fun hook() {
        val clazzOffLineInfoManager = loadClass("com.miui.networkassistant.ui.bean.OffLineInfoManager")
        clazzOffLineInfoManager.findMethod { name("getCachedData") }.createHook {
            returnConstant(null)
        }
        clazzOffLineInfoManager.findMethod { name("getOffLineData") }.createHook {
            returnConstant(null)
        }
    }
}