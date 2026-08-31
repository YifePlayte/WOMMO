package com.yifeplayte.wommo.hook.hooks.singlepackage.packageinstaller

import android.content.pm.ApplicationInfo
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import io.github.lingqiqi5211.ezhooktool.core.findMethod

@Suppress("unused")
object AllowUnofficialSystemApplicationsInstallation : BaseHook() {
    override val key = "allow_unofficial_system_applications_installation"
    override fun hook() {
        dexKitBridge.findMethod {
            matcher {
                paramTypes = listOf("android.content.pm.ApplicationInfo")
                returnType = "boolean"
            }
        }.map { it.getMethodInstance() }.createHooks {
            before { param ->
                (param.args[0] as ApplicationInfo).flags =
                    (param.args[0] as ApplicationInfo).flags.or(ApplicationInfo.FLAG_SYSTEM)
            }
        }
    }
}