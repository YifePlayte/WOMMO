package com.yifeplayte.wommo.hook.hooks.multipackage

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import com.yifeplayte.wommo.hook.utils.AdditionalFields
import com.yifeplayte.wommo.hook.utils.Log
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethodOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

/**
 * Refreshes a third-party widget while it is shown on App Vault.
 *
 * App Vault's only refresh entry, WidgetHostView.onMIUIWidgetUpdate, returns early unless
 * isMIUIWidget is set, so a third-party widget on the vault is never refreshed. Opting the widget
 * into that contract makes App Vault send the refresh, but App Vault then broadcasts
 * android.appwidget.action.APPWIDGET_UPDATE itself. The platform only lets that action target the
 * sender's own components, and answers otherwise with
 * "Permission Denial: not allowed to send broadcast ... to <provider> from com.miui.personalassistant".
 *
 * That check lives in BroadcastController.broadcastIntentLocked and compares the target component's
 * package against callerPackage. The ActivityManagerService method of the same name is a pure
 * forwarder on this platform, which is why hooking it never had any effect. Here only the one shape
 * App Vault sends is re-attributed, to the package App Vault is actually addressing; every other
 * broadcast keeps its original sender.
 */
@Suppress("unused")
object ExposureRefreshForNonMIUIWidget : BaseMultiHook() {
    override val key = "exposure_refresh_for_non_miui_widget"
    override val hooks = mapOf(
        "com.miui.personalassistant" to { appVault() },
        "system" to { system() },
    )

    private const val PACKAGE_APP_VAULT = "com.miui.personalassistant"
    private const val REFRESH_EXPOSURE = "exposure"
    private const val META_DATA_MIUI_WIDGET = "miuiWidget"
    private const val FIELD_IS_REAL_MIUI_WIDGET = "isRealMIUIWidget"
    // BroadcastController.broadcastIntentLocked: callerApp, callerPackage, callerFeatureId, intent.
    private const val BROADCAST_PARAM_COUNT = 25
    private const val INDEX_CALLER_PACKAGE = 1
    private const val INDEX_INTENT = 3
    // Mirrors AppWidgetItemInfo.getMiuiWidgetRefreshMinInterval, which floors the interval at 10s.
    private const val REFRESH_MIN_INTERVAL_MS = 10000

    private val clazzAppWidgetItemInfo by lazy {
        loadClass("com.miui.personalassistant.widget.iteminfo.AppWidgetItemInfo")
    }

    private fun appVault() {
        // parseWidgetMetaData fills the refresh contract for real MIUI widgets only. Record which
        // kind this provider is in a side field, because the parser overwrites isMIUIWidget for the
        // rest and that value cannot be told apart from a real MIUI widget afterwards.
        clazzAppWidgetItemInfo.findMethod { name("parseWidgetMetaData") }.createHook {
            after { param ->
                // The parser returns early when the provider declares no metadata or is not
                // installed, which is the common case for a third-party widget.
                val provider = param.thisObject.getFieldOrNull("provider") as? ComponentName
                val isRealMIUIWidget = provider != null && declaresMiuiWidget(provider)
                AdditionalFields.set(param.thisObject, FIELD_IS_REAL_MIUI_WIDGET, isRealMIUIWidget)
                if (isRealMIUIWidget) return@after
                param.thisObject.putField("isMIUIWidget", true)
                param.thisObject.putField("miuiWidgetRefresh", REFRESH_EXPOSURE)
                param.thisObject.putField("miuiWidgetRefreshMinInterval", REFRESH_MIN_INTERVAL_MS)
            }
        }
        // App Vault always builds the intent with the MIUI action. A third-party provider only
        // handles the platform action, so retarget it before App Vault broadcasts. This runs on
        // every refresh, so it must stay off the package manager and read the recorded flag.
        clazzAppWidgetItemInfo.findMethod { name("obtainMiuiWidgetUpdateIntent") }.createHook {
            after { param ->
                val intent = param.result as? Intent ?: return@after
                if (intent.component == null) return@after
                if (AdditionalFields.get(param.thisObject, FIELD_IS_REAL_MIUI_WIDGET) == true) {
                    return@after
                }
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
        }
    }

    private fun system() {
        val clazzBroadcastController = loadClass("com.android.server.am.BroadcastController")
        val methodBroadcastIntentLocked = clazzBroadcastController.findMethodOrNull {
            name("broadcastIntentLocked")
            paramCount(BROADCAST_PARAM_COUNT)
        } ?: run {
            Log.w("ExposureRefreshForNonMIUIWidget: BroadcastController.broadcastIntentLocked missing")
            return
        }
        // The indices below are only valid for the signature they were read from, so verify the
        // parameter types once instead of trusting the count alone.
        val parameterTypes = methodBroadcastIntentLocked.parameterTypes
        if (parameterTypes.getOrNull(INDEX_CALLER_PACKAGE) != String::class.java ||
            parameterTypes.getOrNull(INDEX_INTENT) != Intent::class.java
        ) {
            Log.w("ExposureRefreshForNonMIUIWidget: unexpected broadcastIntentLocked signature")
            return
        }
        methodBroadcastIntentLocked.createHook {
            before { param ->
                if (param.args[INDEX_CALLER_PACKAGE] != PACKAGE_APP_VAULT) return@before
                val intent = param.args[INDEX_INTENT] as? Intent ?: return@before
                if (intent.action != AppWidgetManager.ACTION_APPWIDGET_UPDATE) return@before
                // The platform check requires callerPackage to equal the target component package.
                val target = intent.component?.packageName ?: return@before
                if (target == PACKAGE_APP_VAULT) return@before
                param.args[INDEX_CALLER_PACKAGE] = target
            }
        }
    }

    /**
     * True only for a provider that declares miuiWidget in its own metadata. Only called while
     * parsing, once per widget, so the package manager round trip stays off the refresh path.
     */
    private fun declaresMiuiWidget(provider: ComponentName): Boolean = runCatching {
        EzXposed.appContext.packageManager.getReceiverInfo(
            provider,
            PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        ).metaData?.getBoolean(META_DATA_MIUI_WIDGET) == true
    }.getOrDefault(false)
}
