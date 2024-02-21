package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.app.Application
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object EnablePerfectIcons : BaseHook() {
    override val key = "enable_perfect_icons"
    override fun hook() {
        runCatching {
            val clazzMiuiSettingsUtils = loadClass("com.miui.launcher.utils.MiuiSettingsUtils")
            loadClass("com.miui.home.launcher.Application").methodFinder().filterByName("disablePerfectIcons").first()
                .createHook {
                    before {
                        val contentResolver = (it.thisObject as Application).contentResolver
                        invokeStaticMethodBestMatch(
                            clazzMiuiSettingsUtils,
                            "putBooleanToSystem",
                            null,
                            contentResolver,
                            "key_miui_mod_icon_enable",
                            true
                        )
                        it.result = null
                    }
                }
        }
        loadClass("miui.content.res.IconCustomizer").methodFinder().filterByName("isModIconEnabledForPackageName")
            .first().createHook { returnConstant(true) }
    }
}