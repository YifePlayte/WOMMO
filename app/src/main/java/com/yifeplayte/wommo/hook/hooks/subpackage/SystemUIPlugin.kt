package com.yifeplayte.wommo.hook.hooks.subpackage

import android.content.pm.ApplicationInfo
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseSubPackage
import com.yifeplayte.wommo.utils.Build.HYPER_OS_VERSION
import com.yifeplayte.wommo.utils.Build.IS_HYPER_OS
import io.github.libxposed.api.XposedInterface

@Suppress("unused")
object SystemUIPlugin : BaseSubPackage("com.android.systemui", "miui.systemui.plugin") {
    var hook: XposedInterface.HookHandle? = null
    override fun initClassLoader() = when {
        HYPER_OS_VERSION >= 4 -> initForHyperOS4()
        IS_HYPER_OS -> initForHyperOS()
        else -> initForMIUI()
    }

    private fun initForHyperOS4() {
        hook =
            loadClass($$"com.android.systemui.shared.plugins.PluginInstance$PluginFactory").findMethod {
                name("createClassLoader"); notAbstract()
            }.createHook {
                after { param ->
                    val appInfo = param.thisObject.getFieldOrNullAs<ApplicationInfo>("pluginAppInfo") ?: return@after
                    if (appInfo.packageName != subPackageName) return@after
                    safeSubClassLoader = param.result as? ClassLoader ?: return@after
                    hook?.unhook()
                }
            }
    }

    private fun initForHyperOS() {
        hook =
            loadClass($$"com.android.systemui.shared.plugins.PluginInstance$PluginFactory").declaredConstructors.single()
                .createHook {
                    before { param ->
                        val appInfo = param.args[2] as ApplicationInfo
                        if (appInfo.packageName != subPackageName) return@before
                        val pathClassLoader = param.args[6]!!.callMethod("get")
                        safeSubClassLoader = pathClassLoader as? ClassLoader ?: return@before
                        hook?.unhook()
                    }
                }
    }

    private fun initForMIUI() {
        hook =
            loadClass($$"com.android.systemui.shared.plugins.PluginInstance$Factory").findMethod {
                name("getClassLoader")
                paramsAssignableFrom(ApplicationInfo::class.java, ClassLoader::class.java)
            }.createHook {
                after { param ->
                    if ((param.args[0] as ApplicationInfo).packageName != subPackageName) return@after
                    safeSubClassLoader = param.result as? ClassLoader ?: return@after
                    hook?.unhook()
                }
            }
    }
}
