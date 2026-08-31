package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object QsTileLongPressFallbackOverride : BaseHook() {
    override val key = "qs_tile_long_press_fallback_override"
    override fun hook() {
        loadClass("com.android.systemui.qs.external.CustomTile").findMethod {
            name("getLongClickIntent"); notAbstract()
        }.createHook {
            after { param ->
                val result = param.result as? Intent ?: return@after
                if (Settings.ACTION_APPLICATION_DETAILS_SETTINGS != result.action) return@after
                val mCustomTileExt = param.thisObject.getFieldOrNull("mCustomTileExt") ?: return@after
                val context = mCustomTileExt.getFieldOrNullAs<Context>("userContext") ?: return@after
                val user = context.callMethod("getUser") as UserHandle
                val componentName = mCustomTileExt.getFieldOrNullAs<ComponentName>("componentName") ?: return@after
                val intent = context.packageManager.getLaunchIntentForPackage(componentName.packageName) ?: return@after
                intent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(Intent.EXTRA_USER, user)
                }
                if (loadClass("com.miui.systemui.controlcenter.utils.ControlCenterUtils").callStaticMethod("useSplitSettings") as Boolean) {
                    intent.callMethod("addMiuiFlags", 8)
                }
                param.result = intent
            }
        }
    }
}
