package com.yifeplayte.wommo.hook.hooks.singlepackage.system

import android.content.pm.ApplicationInfo
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD
import com.yifeplayte.wommo.utils.Clazz.setStaticFinalObject
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods

@Suppress("unused")
object ForceDarkModeForAllApps : BaseHook() {
    override val key = "force_dark_mode_for_all_apps"
    override val isEnabled get() = !IS_INTERNATIONAL_BUILD && super.isEnabled
    private val clazzBuild by lazy { loadClass("miui.os.Build") }
    override fun hook() {
        val clazzForceDarkAppListManager = loadClass("com.android.server.ForceDarkAppListManager")
        clazzForceDarkAppListManager.findAllMethods { name("getDarkModeAppList") }
            .createHooks {
                before {
                    if (!IS_INTERNATIONAL_BUILD)
                        setStaticFinalObject(clazzBuild, "IS_INTERNATIONAL_BUILD", true)
                }
                after {
                    setStaticFinalObject(clazzBuild, "IS_INTERNATIONAL_BUILD", IS_INTERNATIONAL_BUILD)
                }
            }
        clazzForceDarkAppListManager.findAllMethods { name("shouldShowInSettings") }
            .createHooks {
                before { param ->
                    val info = param.args[0] as ApplicationInfo?
                    param.result = !(info == null || (info.callMethod(
                        "isSystemApp"
                    ) as Boolean) || info.uid < 10000)
                }
            }
    }
}