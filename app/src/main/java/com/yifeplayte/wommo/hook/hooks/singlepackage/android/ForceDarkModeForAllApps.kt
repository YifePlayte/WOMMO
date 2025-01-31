package com.yifeplayte.wommo.hook.hooks.singlepackage.android

import android.content.pm.ApplicationInfo
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD

@Suppress("unused")
object ForceDarkModeForAllApps : BaseHook() {
    override val key = "force_dark_mode_for_all_apps"
    override val isEnabled get() = !IS_INTERNATIONAL_BUILD && super.isEnabled
    private val clazzBuild by lazy { loadClass("miui.os.Build") }
    override fun hook() {
        val clazzForceDarkAppListManager = loadClass("com.android.server.ForceDarkAppListManager")
        clazzForceDarkAppListManager.methodFinder().filterByName("getDarkModeAppList").toList()
            .createHooks {
                before {
                    setStaticObject(clazzBuild, "IS_INTERNATIONAL_BUILD", true)
                }
                after {
                    setStaticObject(clazzBuild, "IS_INTERNATIONAL_BUILD", IS_INTERNATIONAL_BUILD)
                }
            }
        clazzForceDarkAppListManager.methodFinder().filterByName("shouldShowInSettings").toList()
            .createHooks {
                before { param ->
                    val info = param.args[0] as ApplicationInfo?
                    param.result = !(info == null || (invokeMethodBestMatch(
                        info, "isSystemApp"
                    ) as Boolean) || info.uid < 10000)
                }
            }
    }
}