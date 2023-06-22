package com.yifeplayte.wommo.hook.hooks.singlepackage.packageinstaller

import android.content.pm.ApplicationInfo
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.loadDexKit

object AllowUnofficialSystemApplicationsInstallation : BaseHook() {
    override val key = "allow_unofficial_system_applications_installation"
    override fun hook() {
        loadDexKit()
        dexKitBridge.findMethod {
            methodParamTypes = arrayOf("Landroid/content/pm/ApplicationInfo;")
            methodReturnType = "boolean"
        }.forEach {
            it.getMethodInstance(EzXHelper.safeClassLoader).createHook {
                before { param ->
                    (param.args[0] as ApplicationInfo).flags =
                        (param.args[0] as ApplicationInfo).flags.or(ApplicationInfo.FLAG_SYSTEM)
                }
            }
        }
    }
}