package com.yifeplayte.wommo.hook.hooks.singlepackage.settings

import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD

@Suppress("unused")
object ShowGoogleSettingsEntry : BaseHook() {
    override val key = "show_google_settings_entry"
    override val isEnabled get() = !IS_INTERNATIONAL_BUILD && super.isEnabled
    override fun hook() {
        loadClass("com.android.settings.MiuiSettings").findMethod {
            name("updateHeaderList")
        }.createHook {
            after {
                if (!IS_INTERNATIONAL_BUILD) it.thisObject.callMethod(
                    "AddGoogleSettingsHeaders", it.args[0]
                )
            }
        }
    }
}