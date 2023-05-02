package com.yifeplayte.wommo.hook

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.hooks.home.AddFreeformShortcut
import com.yifeplayte.wommo.hook.hooks.home.RestoreGoogleAppIcon
import com.yifeplayte.wommo.hook.hooks.screenrecorder.EnablePlaybackCapture
import com.yifeplayte.wommo.hook.hooks.screenrecorder.ModifyScreenRecorderConfig
import com.yifeplayte.wommo.hook.hooks.systemui.RestoreNearbyTile
import com.yifeplayte.wommo.hook.utils.DexKit
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getBoolean
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

private const val TAG = "WOMMO"
val PACKAGE_NAME_HOOKED = listOf(
    "com.miui.screenrecorder",
    "com.android.systemui",
    "com.miui.home"
)

@Suppress("unused")
class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName in PACKAGE_NAME_HOOKED) {
            if (lpparam.packageName != "android") DexKit.initDexKit(lpparam)
            EzXHelper.initHandleLoadPackage(lpparam)
            EzXHelper.setLogTag(TAG)
            EzXHelper.setToastTag(TAG)
            when (EzXHelper.hostPackageName) {
                "com.miui.screenrecorder" -> {
                    initHook(EnablePlaybackCapture, "force_enable_native_playback_capture")
                    initHook(ModifyScreenRecorderConfig, "modify_screen_recorder_config")
                }

                "com.android.systemui" -> {
                    initHook(RestoreNearbyTile, "restore_near_by_tile")
                }

                "com.miui.home" -> {
                    initHook(RestoreGoogleAppIcon, "restore_google_app_icon")
                    initHook(AddFreeformShortcut, "add_freeform_shortcut")
                }
            }
            DexKit.closeDexKit()
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelper.initZygote(startupParam)
    }

    private fun initHook(hook: BaseHook, key: String, defValue: Boolean = false) =
        initHook(hook, getBoolean(key, defValue))

    private fun initHook(hook: BaseHook, enable: Boolean = true) {
        if (enable) runCatching {
            if (hook.isInit) return
            hook.init()
            hook.isInit = true
            Log.ix("Inited hook: ${hook.javaClass.simpleName}")
        }.logexIfThrow("Failed init hook: ${hook.javaClass.simpleName}")
    }
}
