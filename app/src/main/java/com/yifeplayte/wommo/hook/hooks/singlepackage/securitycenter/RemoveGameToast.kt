package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import io.github.lingqiqi5211.ezhooktool.core.findMethod

@Suppress("unused")
object RemoveGameToast : BaseHook() {
    override val key = "remove_game_toast"
    override fun hook() {
        runCatching {
            dexKitBridge.findMethod {
                matcher {
                    usingStrings = listOf("NewDockGameToast method: ")
                }
            }.single().getMethodInstance().createHook {
                returnConstant(true)
            }
        }
        runCatching {
            dexKitBridge.findMethod {
                matcher {
                    usingStrings = listOf("showNewWindowToastView: ")
                }
            }.single().getMethodInstance().createHook {
                returnConstant(null)
            }
        }
        runCatching {
            dexKitBridge.findMethod {
                matcher {
                    usingStrings = listOf("showNewWindowToastView")
                }
            }.single().getMethodInstance().createHook {
                returnConstant(null)
            }
        }
        runCatching {
            dexKitBridge.findMethod {
                matcher {
                    usingStrings = listOf("showWildModeToastView: ")
                }
            }.single().getMethodInstance().createHook {
                returnConstant(null)
            }
        }
        runCatching {
            dexKitBridge.findMethod {
                matcher {
                    usingStrings = listOf("cancel game toast , isCanceled : ")
                }
            }.single().getMethodInstance().createHook {
                returnConstant(null)
            }
        }
    }
}