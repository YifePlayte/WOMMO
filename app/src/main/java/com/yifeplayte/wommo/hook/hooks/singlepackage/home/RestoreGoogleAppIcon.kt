package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.content.ComponentName
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object RestoreGoogleAppIcon : BaseHook() {
    override val key = "restore_google_app_icon"
    override fun hook() {
        loadClass("com.miui.home.launcher.AppFilter").declaredConstructors.toList().createHooks {
            after { param ->
                param.thisObject.getFieldOrNullAs<HashSet<ComponentName>>(
                    "mSkippedItems"
                )!!.removeIf {
                    it.packageName in setOf(
                        "com.google.android.googlequicksearchbox",
                        "com.google.android.gms"
                    )
                }
            }
        }
    }
}