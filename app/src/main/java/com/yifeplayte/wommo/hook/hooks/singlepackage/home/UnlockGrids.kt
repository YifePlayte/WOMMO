package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook


@Suppress("unused")
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
            it.findMethod { name("getCellCountXMin"); findAndSuper() }.createHook {
                returnConstant(4)
            }
            it.findMethod { name("getCellCountYMin"); findAndSuper() }.createHook {
                returnConstant(6)
            }
            it.findMethod { name("getCellCountXMax"); findAndSuper() }.createHook {
                returnConstant(16)
            }
            it.findMethod { name("getCellCountYMax"); findAndSuper() }.createHook {
                returnConstant(18)
            }
        }
    }
}