package com.yifeplayte.wommo.hook.hooks.singlepackage.barrage

import android.content.ContentResolver
import android.provider.Settings
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object GlobalBarrage : BaseHook() {
    override val key = "global_barrage"
    override fun hook() {
        loadClass($$"android.provider.Settings$Secure").findAllMethods {
            name("getInt")
        }.createHooks {
            after { param ->
                if ((param.args[1] as String) == "gb_boosting" && param.result != 1) {
                    Settings.Secure.putInt(param.args[0] as ContentResolver?, "gb_boosting", 1)
                    param.result = 1
                }
            }
        }
    }
}