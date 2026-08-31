package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.app.Application
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object EnablePerfectIcons : BaseHook() {
    override val key = "enable_perfect_icons"
    override fun hook() {
        runCatching {
            val clazzMiuiSettingsUtils = loadClass("com.miui.launcher.utils.MiuiSettingsUtils")
            loadClass("com.miui.home.launcher.Application").findMethod {
                name("disablePerfectIcons")
            }.createHook {
                before {
                    val contentResolver = (it.thisObject as Application).contentResolver
                    clazzMiuiSettingsUtils.callStaticMethod(
                        "putBooleanToSystem",
                        contentResolver,
                        "key_miui_mod_icon_enable",
                        true
                    )
                    it.result = null
                }
            }
        }
        loadClass("miui.content.res.IconCustomizer").findMethod {
            name("isModIconEnabledForPackageName")
        }.createHook { returnConstant(true) }
    }
}