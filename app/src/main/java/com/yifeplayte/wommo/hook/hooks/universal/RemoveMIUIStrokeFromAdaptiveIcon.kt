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
            loadClass("android.app.Activity").methodFinder().filterByName("onCreate").first().createHook {
                before {
                    val clazzAdaptiveIconDrawableInjector =
                        loadClass("android.graphics.drawable.AdaptiveIconDrawableInjector")
                    clazzAdaptiveIconDrawableInjector.methodFinder().filterByName("drawMiuiStroke").toList()
                        .createHooks {
                            returnConstant(null)
                        }
                }
            }
        }

        loadClass("android.app.Activity").methodFinder().filterByName("onCreate").first().createHook {
            before {
                val clazzAdaptiveIconDrawableInjector =
                    loadClass("android.graphics.drawable.AdaptiveIconDrawableInjector")
                clazzAdaptiveIconDrawableInjector.methodFinder().filterByName("drawMiuiStroke").toList().createHooks {
                    returnConstant(null)
                }
            }
        }
    }
}