package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook


object UnlockGrids : BaseHook() {
    override val key = "unlock_grids"
    override fun hook() {
        val clazzSet = setOf(
            loadClass("com.miui.home.launcher.compat.LauncherCellCountCompatDevice"),
            loadClass("com.miui.home.launcher.compat.LauncherCellCountCompatJP"),
            loadClass("com.miui.home.launcher.compat.LauncherCellCountCompatNoWord"),
            loadClass("com.miui.home.launcher.compat.LauncherCellCountCompatResource")
        )
        clazzSet.forEach {
            it.methodFinder().findSuper().filterByName("getCellCountXMin").first().createHook {
                returnConstant(4)
            }
            it.methodFinder().findSuper().filterByName("getCellCountYMin").first().createHook {
                returnConstant(6)
            }
            it.methodFinder().findSuper().filterByName("getCellCountXMax").first().createHook {
                returnConstant(16)
            }
            it.methodFinder().findSuper().filterByName("getCellCountYMax").first().createHook {
                returnConstant(18)
            }
        }
    }
}