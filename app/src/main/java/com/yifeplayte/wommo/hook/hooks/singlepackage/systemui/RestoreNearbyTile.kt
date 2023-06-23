package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD

object RestoreNearbyTile : BaseHook() {
    override val key = "restore_near_by_tile"
    override fun hook() {
        if (IS_INTERNATIONAL_BUILD) return
        val isInternationalHook: HookFactory.() -> Unit = {
            val constantsClazz = loadClass("com.android.systemui.controlcenter.utils.Constants")
            before {
                setStaticObject(constantsClazz, "IS_INTERNATIONAL", true)
            }
            after {
                setStaticObject(constantsClazz, "IS_INTERNATIONAL", false)
            }
        }

        loadClass("com.android.systemui.qs.MiuiQSTileHostInjector").methodFinder().first {
            name == "createMiuiTile"
        }.createHook(isInternationalHook)

        loadClass("com.android.systemui.controlcenter.utils.ControlCenterUtils").methodFinder().first {
            name == "filterCustomTile"
        }.createHook(isInternationalHook)
    }
}