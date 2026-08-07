package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object QsTileLongPressFallbackOverride : BaseHook() {
    override val key = "qs_tile_long_press_fallback_override"
    override fun hook() {
        loadClass("com.android.systemui.qs.external.CustomTile")
            .methodFinder()
            .filterByName("getLongClickIntent")
            .filterNonAbstract()
            .single()
            .createHook {
                after { param ->
                    val result = param.result as? Intent ?: return@after
                    if (Settings.ACTION_APPLICATION_DETAILS_SETTINGS != result.action) return@after
                    val mCustomTileExt = getObjectOrNull(param.thisObject, "mCustomTileExt") ?: return@after
                    val context = getObjectOrNullAs<Context>(mCustomTileExt, "userContext") ?: return@after
                    val user = invokeMethodBestMatch(context, "getUser") as UserHandle
                    val componentName = getObjectOrNullAs<ComponentName>(mCustomTileExt, "componentName") ?: return@after
                    val intent = context.packageManager.getLaunchIntentForPackage(componentName.packageName) ?: return@after
                    intent.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(Intent.EXTRA_USER, user)
                    }
                    if (invokeStaticMethodBestMatch(loadClass("com.miui.systemui.controlcenter.utils.ControlCenterUtils"), "useSplitSettings") as Boolean) {
                        invokeMethodBestMatch(intent, "addMiuiFlags", null, 8)
                    }
                    param.result = intent
                }
            }
    }
}