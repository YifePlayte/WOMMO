package com.yifeplayte.wommo.hook.hooks.singlepackage.contacts

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getInstance

@Suppress("unused")
object DisableBusinessHallOfflineInfoManager : BaseHook() {
    override val key = "disable_business_hall_offline_info_manager"
    override fun hook() {
        val clazzOffLineInfoManager = dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("OFF_LINE_DATA_CACHE")
            }
        }.single().getInstance()
        val clazzOffLineData = loadClass("com.mobile.businesshall.bean.OffLineData")
        clazzOffLineInfoManager.findMethod { returnType(clazzOffLineData) }.createHook {
            returnConstant(null)
        }
        clazzOffLineInfoManager.findMethod { paramCount(0); returnType(Void.TYPE) }.createHook {
            returnConstant(null)
        }
    }
}