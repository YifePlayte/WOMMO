package com.yifeplayte.wommo.hook.hooks.subpackage

import android.content.pm.ApplicationInfo
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseSubPackage
import com.yifeplayte.wommo.hook.hooks.subpackage.systemuiplugin.RestoreNearbyTile
import de.robv.android.xposed.XC_MethodHook.Unhook

object SystemUIPlugin : BaseSubPackage() {
    override val packageName = "com.android.systemui"
    override val subPackageName = "miui.systemui.plugin"
    override val hooks = setOf(
        RestoreNearbyTile
    )
    var hook: Unhook? = null
    override fun initClassLoader() {
        hook = loadClass("com.android.systemui.shared.plugins.PluginInstance\$Factory").methodFinder()
            .filterByName("getClassLoader")
            .filterByAssignableParamTypes(ApplicationInfo::class.java, ClassLoader::class.java)
            .first().createHook {
                after { param ->
                    if ((param.args[0] as ApplicationInfo).packageName != subPackageName) return@after
                    safeSubClassLoader = param.result as? ClassLoader ?: return@after
                    hook?.unhook()
                }
            }
    }
}