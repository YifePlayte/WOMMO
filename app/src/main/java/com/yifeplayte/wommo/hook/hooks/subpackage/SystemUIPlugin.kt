package com.yifeplayte.wommo.hook.hooks.subpackage

import android.content.pm.ApplicationInfo
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseSubPackage
import com.yifeplayte.wommo.utils.Build.HYPER_OS_VERSION
import com.yifeplayte.wommo.utils.Build.IS_HYPER_OS
import de.robv.android.xposed.XC_MethodHook.Unhook

@Suppress("unused")
object SystemUIPlugin : BaseSubPackage("com.android.systemui", "miui.systemui.plugin") {
    var hook: Unhook? = null
    override fun initClassLoader() = when {
        HYPER_OS_VERSION >= 4 -> initForHyperOS4()
        IS_HYPER_OS -> initForHyperOS()
        else -> initForMIUI()
    }

    private fun initForHyperOS4() {
        hook =
            loadClass($$"com.android.systemui.shared.plugins.PluginInstance$PluginFactory").methodFinder()
                .filterByName("createClassLoader")
                .filterNonAbstract()
                .single()
                .createHook {
                    after { param ->
                        val appInfo = getObjectOrNullAs<ApplicationInfo>(param.thisObject, "pluginAppInfo") ?: return@after
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
                        val pathClassLoader = invokeMethodBestMatch(param.args[6], "get")
                        safeSubClassLoader = pathClassLoader as? ClassLoader ?: return@before
                        hook?.unhook()
                    }
                }
    }

    private fun initForMIUI() {
        hook =
            loadClass($$"com.android.systemui.shared.plugins.PluginInstance$Factory").methodFinder()
                .filterByName("getClassLoader")
                .filterByAssignableParamTypes(
                    ApplicationInfo::class.java,
                    ClassLoader::class.java
                )
                .single().createHook {
                    after { param ->
                        if ((param.args[0] as ApplicationInfo).packageName != subPackageName) return@after
                        safeSubClassLoader = param.result as? ClassLoader ?: return@after
                        hook?.unhook()
                    }
                }
    }
}