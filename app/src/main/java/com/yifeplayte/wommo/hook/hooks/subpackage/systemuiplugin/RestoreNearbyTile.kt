package com.yifeplayte.wommo.hook.hooks.subpackage.systemuiplugin

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseSubHook

@Suppress("unused")
object RestoreNearbyTile : BaseSubHook() {
    override val key = "restore_near_by_tile"
    override fun hook(subClassLoader: ClassLoader) {
        loadClass(
            "miui.systemui.controlcenter.qs.customize.TileQueryHelper\$Companion",
            subClassLoader
        ).methodFinder()
            .filterByName("filterNearby").single().createHook {
                returnConstant(false)
            }
    }
}