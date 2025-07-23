package com.yifeplayte.wommo.hook.hooks.singlepackage.voiceassist

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

@Suppress("unused")
object EnableWakeUpAdvancedAnimation : BaseHook() {
    override val key = "enable_wake_up_advanced_animation"
    override fun hook() {
        val clazzSystemProperties = loadClass("miuix.core.util.SystemProperties")
        clazzSystemProperties.methodFinder()
            .filterByName("get")
            .filterByParamCount(2)
            .filterNonAbstract()
            .single()
            .createHook {
                before {
                    if (it.args[0] == "persist.sys.background_blur_supported") {
                        it.result = "true"
                    }
                }
            }
        clazzSystemProperties.methodFinder()
            .filterByName("getBoolean")
            .filterByParamCount(2)
            .filterNonAbstract()
            .single()
            .createHook {
                before {
                    if (it.args[0] == "persist.sys.background_blur_supported") {
                        it.result = true
                    }
                }
            }
        val clazzWakeUpAnimHelper = dexKitBridge.findClass {
            matcher {
                usingStrings = listOf(
                    "WakeUpAnimHelper",
                    "persist.sys.background_blur_supported",
                    "isN8DeviceCpuOverHot: isDeviceN8 = "
                )
            }
        }.single().getInstance(safeClassLoader)
        clazzWakeUpAnimHelper.methodFinder()
            .filterByName("isDeviceNeedBoostGpu")
            .filterNonAbstract()
            .single()
            .createHook {
                returnConstant(true)
            }
        clazzWakeUpAnimHelper.methodFinder()
            .filterByName("isDeviceOpenAdvanceAnim")
            .filterNonAbstract()
            .single()
            .createHook {
                returnConstant(true)
            }
    }
}