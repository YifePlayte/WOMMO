package com.yifeplayte.wommo.hook.hooks.singlepackage.voiceassist

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getInstance

@Suppress("unused")
object EnableWakeUpAdvancedAnimation : BaseHook() {
    override val key = "enable_wake_up_advanced_animation"
    override fun hook() {
        val clazzSystemProperties = loadClass("miuix.core.util.SystemProperties")
        clazzSystemProperties.findMethod {
            name("get")
            paramCount(2)
            notAbstract()
        }.createHook {
            before {
                if (it.args[0] == "persist.sys.background_blur_supported") {
                    it.result = "true"
                }
            }
        }
        clazzSystemProperties.findMethod {
            name("getBoolean")
            paramCount(2)
            notAbstract()
        }.createHook {
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
        }.single().getInstance()
        clazzWakeUpAnimHelper.findMethod {
            name("isDeviceNeedBoostGpu")
            notAbstract()
        }.createHook {
            returnConstant(true)
        }
        clazzWakeUpAnimHelper.findMethod {
            name("isDeviceOpenAdvanceAnim")
            notAbstract()
        }.createHook {
            returnConstant(true)
        }
    }
}