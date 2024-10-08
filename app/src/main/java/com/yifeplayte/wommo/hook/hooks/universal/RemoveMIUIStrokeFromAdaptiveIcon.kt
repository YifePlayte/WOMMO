package com.yifeplayte.wommo.hook.hooks.universal

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseUniversalHook

@Suppress("unused")
object RemoveMIUIStrokeFromAdaptiveIcon : BaseUniversalHook() {
    override val key = "remove_miui_stroke_from_adaptive_icon"
    override fun hook() {
        runCatching {
            val clazzAdaptiveIconDrawableInjector =
                loadClass("android.graphics.drawable.AdaptiveIconDrawableInjector")
            clazzAdaptiveIconDrawableInjector.methodFinder()
                .filterByName("drawMiuiStroke").filterNonAbstract().toList()
                .createHooks {
                    returnConstant(null)
                }
            clazzAdaptiveIconDrawableInjector.methodFinder()
                .filterByName("drawMiuiFullStroke").filterNonAbstract().toList()
                .createHooks {
                    returnConstant(null)
                }
        }
    }
}