package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object DisableNetworkAssistantOfflineInfoManager : BaseHook() {
    override val key = "disable_network_assistant_offline_info_manager"
    override fun hook() {
        val clazzOffLineInfoManager = loadClass("com.miui.networkassistant.ui.bean.OffLineInfoManager")
        clazzOffLineInfoManager.methodFinder()
            .filterByName("getCachedData").single().createHook {
                returnConstant(null)
            }
        clazzOffLineInfoManager.methodFinder()
            .filterByName("getOffLineData").single().createHook {
                returnConstant(null)
            }
    }
}