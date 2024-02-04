package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD
import java.lang.reflect.Method

object RestoreNearbyTile : BaseHook() {
    override val key = "restore_near_by_tile"
    override val isEnabled get() = !IS_INTERNATIONAL_BUILD and super.isEnabled
    override fun hook() {
        val isInternationalHook: HookFactory.() -> Unit = {
            val constantsClazz = loadClass("com.android.systemui.controlcenter.utils.Constants")
            before {
                setStaticObject(constantsClazz, "IS_INTERNATIONAL", true)
            }
            after {
                setStaticObject(constantsClazz, "IS_INTERNATIONAL", false)
            }
        }

        val hookSet = mutableSetOf<Method>()

        loadClassOrNull("com.android.systemui.qs.MiuiQSTileHostInjector")?.methodFinder()
            ?.filterByName("createMiuiTile")?.toList()?.let { hookSet.addAll(it) }

        loadClassOrNull("com.android.systemui.controlcenter.utils.ControlCenterUtils")?.methodFinder()
            ?.filterByName("filterCustomTile")?.toList()?.let { hookSet.addAll(it) }

        hookSet.createHooks(isInternationalHook)
    }
}