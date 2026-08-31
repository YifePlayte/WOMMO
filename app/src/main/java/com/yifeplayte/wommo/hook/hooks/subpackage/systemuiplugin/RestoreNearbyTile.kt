package com.yifeplayte.wommo.hook.hooks.subpackage.systemuiplugin

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseSubHook

@Suppress("unused")
object RestoreNearbyTile : BaseSubHook() {
    override val key = "restore_near_by_tile"
    override fun hook(subClassLoader: ClassLoader) {
        loadClass(
            $$"miui.systemui.controlcenter.qs.customize.TileQueryHelper$Companion",
            subClassLoader
        ).findMethod { name("filterNearby") }.createHook {
            returnConstant(false)
        }
    }
}
