package com.yifeplayte.wommo.hook.hooks.singlepackage.screenrecorder

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.isFinal
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

object ModifyScreenRecorderConfig : BaseHook() {
    override val key = "modify_screen_recorder_config"
    override fun hook() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("Error when set frame value, maxValue = ")
                paramTypes = listOf("int", "int")
            }
        }.first().getMethodInstance(safeClassLoader).createHook {
            before { param ->
                param.args[0] = 3600
                param.args[1] = 1
                param.method.declaringClass.declaredFields.firstOrNull { field ->
                    field.also {
                        it.isAccessible = true
                    }.let { fieldAccessible ->
                        fieldAccessible.isFinal && fieldAccessible.get(null).let {
                            kotlin.runCatching {
                                (it as IntArray).contentEquals(intArrayOf(15, 24, 30, 48, 60, 90))
                            }.getOrDefault(false)
                        }
                    }
                }?.set(null, intArrayOf(15, 24, 30, 48, 60, 90, 120, 144))
            }
        }
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("defaultBitRate = ")
                paramCount = 2
                paramTypes = listOf("int", "int")
            }
        }.firstOrNull()?.getMethodInstance(safeClassLoader)?.createHook {
            before { param ->
                param.args[0] = 3600
                param.args[1] = 1
                param.method.declaringClass.declaredFields.firstOrNull { field ->
                    field.also {
                        it.isAccessible = true
                    }.let { fieldAccessible ->
                        fieldAccessible.isFinal && fieldAccessible.get(null).let {
                            kotlin.runCatching {
                                (it as IntArray).contentEquals(
                                    intArrayOf(
                                        200, 100, 50, 32, 24, 16, 8, 6, 4, 1
                                    )
                                )
                            }.getOrDefault(false)
                        }
                    }
                }?.set(
                    null,
                    intArrayOf(3600, 2400, 1200, 800, 400, 200, 100, 50, 32, 24, 16, 8, 6, 4, 1)
                )
            }
        }
    }
}