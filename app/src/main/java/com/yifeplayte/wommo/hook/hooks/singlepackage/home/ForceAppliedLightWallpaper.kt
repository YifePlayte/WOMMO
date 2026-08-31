package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethodOrNull
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object ForceAppliedLightWallpaper : BaseHook() {
    override val key = "force_applied_light_wallpaper"
    override fun hook() {
        val clazzWallpaperUtils = loadClass("com.miui.home.launcher.WallpaperUtils")
        clazzWallpaperUtils.findMethod {
            name("hasLightBgForStatusBar"); notAbstract()
        }.createHook {
            returnConstant(true)
        }
        clazzWallpaperUtils.findMethodOrNull {
            name("hasAppliedLightWallpaper"); notAbstract()
        }?.createHook {
            returnConstant(true)
        }
        loadClassOrNull("com.miui.home.isolate.wallpaper.WallpaperUtil")?.findMethodOrNull {
            name("hasAppliedLightWallpaper"); notAbstract()
        }?.createHook {
            returnConstant(true)
        }
    }
}