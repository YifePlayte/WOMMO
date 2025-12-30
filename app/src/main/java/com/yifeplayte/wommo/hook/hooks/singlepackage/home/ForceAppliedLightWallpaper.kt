package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object ForceAppliedLightWallpaper : BaseHook() {
    override val key = "force_applied_light_wallpaper"
    override fun hook() {
        val clazzWallpaperUtils = loadClass("com.miui.home.launcher.WallpaperUtils")
        clazzWallpaperUtils.methodFinder()
            .filterByName("hasLightBgForStatusBar").filterNonAbstract().single().createHook {
                returnConstant(true)
            }
        clazzWallpaperUtils.methodFinder()
            .filterByName("hasAppliedLightWallpaper").filterNonAbstract().singleOrNull()?.createHook {
                returnConstant(true)
            }
        loadClassOrNull("com.miui.home.isolate.wallpaper.WallpaperUtil")?.methodFinder()
            ?.filterByName("hasAppliedLightWallpaper")?.filterNonAbstract()?.single()?.createHook {
                returnConstant(true)
            }
    }
}