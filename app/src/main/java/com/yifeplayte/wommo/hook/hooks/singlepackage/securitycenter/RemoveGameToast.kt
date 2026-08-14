package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance

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