package com.yifeplayte.wommo.hook.hooks.singlepackage.screenrecorder

import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import java.lang.reflect.Modifier
import io.github.lingqiqi5211.ezhooktool.core.findMethod

@Suppress("unused")
object ModifyScreenRecorderConfig : BaseHook() {
    override val key = "modify_screen_recorder_config"
    private val intArrayBitRateOld = intArrayOf(200, 100, 50, 32, 24, 16, 8, 6, 4, 1)
    private val intArrayBitRateNew =
        intArrayOf(3600, 2400, 1200, 800, 400, 200, 100, 50, 32, 24, 16, 8, 6, 4, 1)
    private val intArrayFrameRateOld = intArrayOf(15, 24, 30, 48, 60, 90)
    private val intArrayFrameRateNew = intArrayOf(15, 24, 30, 48, 60, 90, 120, 144)

    override fun hook() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("Error when set frame value, maxValue = ")
                paramTypes = listOf("int", "int")
            }
        }.single().getMethodInstance().createHook {
            before { param ->
                param.args[0] = 3600
                param.args[1] = 1
                param.executable.declaringClass.declaredFields.firstOrNull { field ->
                    field.also {
                        it.isAccessible = true
                    }.let { fieldAccessible ->
                        Modifier.isFinal(fieldAccessible.modifiers) && fieldAccessible.get(null).let {
                            runCatching {
                                (it as IntArray).contentEquals(intArrayFrameRateOld)
                            }.getOrDefault(false)
                        }
                    }
                }?.set(null, intArrayFrameRateNew)
            }
        }
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("defaultBitRate = ")
                paramCount = 2
                paramTypes = listOf("int", "int")
            }
        }.singleOrNull()?.getMethodInstance()?.createHook {
            before { param ->
                param.args[0] = 3600
                param.args[1] = 1
                param.executable.declaringClass.declaredFields.firstOrNull { field ->
                    field.also {
                        it.isAccessible = true
                    }.let { fieldAccessible ->
                        Modifier.isFinal(fieldAccessible.modifiers) && fieldAccessible.get(null).let {
                            runCatching {
                                (it as IntArray).contentEquals(intArrayBitRateOld)
                            }.getOrDefault(false)
                        }
                    }
                }?.set(null, intArrayBitRateNew)
            }
        }
    }
}