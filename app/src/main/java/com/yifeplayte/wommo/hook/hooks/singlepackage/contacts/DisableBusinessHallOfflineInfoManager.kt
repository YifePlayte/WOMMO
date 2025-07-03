package com.yifeplayte.wommo.hook.hooks.singlepackage.contacts

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object DisableBusinessHallOfflineInfoManager : BaseHook() {
    override val key = "disable_business_hall_offline_info_manager"
    override fun hook() {
        val clazzOffLineInfoManager = loadClass("com.mobile.businesshall.control.OffLineInfoManager")
        val clazzOffLineData = loadClass("com.mobile.businesshall.bean.OffLineData")
        clazzOffLineInfoManager.methodFinder()
            .filterByReturnType(clazzOffLineData).single().createHook {
                returnConstant(null)
            }
        clazzOffLineInfoManager.methodFinder()
            .filterByParamCount(0).filterByReturnType(Void.TYPE).single().createHook {
                returnConstant(null)
            }
    }
}