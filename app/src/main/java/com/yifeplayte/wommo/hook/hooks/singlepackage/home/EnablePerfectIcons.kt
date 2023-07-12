package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.app.Application
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

object EnablePerfectIcons : BaseHook() {
    override val key = "enable_perfect_icons"
    override fun hook() {
        val clazzMiuiSettingsUtils = loadClass("com.miui.launcher.utils.MiuiSettingsUtils")
        loadClass("com.miui.home.launcher.Application").methodFinder().filterByName("disablePerfectIcons").first()
            .createHook {
                replace {
                    val contentResolver = (it.thisObject as Application).contentResolver
                    clazzMiuiSettingsUtils.methodFinder().filterByName("putBool")
                    invokeStaticMethodBestMatch(
                        clazzMiuiSettingsUtils,
                        "putBooleanToSystem",
                        null,
                        contentResolver,
                        "key_miui_mod_icon_enable",
                        true
                    )
                }
            }
    }
}