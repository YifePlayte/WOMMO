package com.yifeplayte.wommo.hook

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.hooks.home.AddFreeformShortcut
import com.yifeplayte.wommo.hook.hooks.home.AllowMoveNonMIUIWidgetToMinusScreen
import com.yifeplayte.wommo.hook.hooks.home.RestoreGoogleAppIcon
import com.yifeplayte.wommo.hook.hooks.home.RestoreSwitchMinusScreen
import com.yifeplayte.wommo.hook.hooks.packageinstaller.AllowUnofficialSystemApplicationsInstallation
import com.yifeplayte.wommo.hook.hooks.personalassistant.ExposureRefreshForNonMIUIWidget
import com.yifeplayte.wommo.hook.hooks.screenrecorder.EnablePlaybackCapture
import com.yifeplayte.wommo.hook.hooks.screenrecorder.ModifyScreenRecorderConfig
import com.yifeplayte.wommo.hook.hooks.securitycenter.OpenByDefaultSetting
import com.yifeplayte.wommo.hook.hooks.systemui.LockscreenChargingInfo
import com.yifeplayte.wommo.hook.hooks.systemui.NotificationSettingsNoWhiteList
import com.yifeplayte.wommo.hook.hooks.systemui.RestoreNearbyTile
import com.yifeplayte.wommo.hook.hooks.systemui.WaveCharge
import com.yifeplayte.wommo.hook.utils.DexKit
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getBoolean
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

private const val TAG = "WOMMO"
val PACKAGE_NAME_HOOKED = listOf(
    "com.miui.screenrecorder",
    "com.android.systemui",
    "com.miui.home",
    "com.miui.securitycenter",
    "com.miui.packageinstaller",
    "com.miui.personalassistant",
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
                    initHook(NotificationSettingsNoWhiteList, "notification_settings_no_white_list")
                    initHook(LockscreenChargingInfo, "lockscreen_charging_info")
                    initHook(WaveCharge, "wave_charge")
                }

                "com.miui.home" -> {
                    initHook(RestoreGoogleAppIcon, "restore_google_app_icon")
                    initHook(AddFreeformShortcut, "add_freeform_shortcut")
                    initHook(RestoreSwitchMinusScreen, "restore_switch_minus_screen")
                    initHook(AllowMoveNonMIUIWidgetToMinusScreen, "allow_move_non_miui_widget_to_minus_screen")
                }

                "com.miui.securitycenter" -> {
                    initHook(OpenByDefaultSetting, "open_by_default_setting")
                }

                "com.miui.packageinstaller" -> {
                    initHook(
                        AllowUnofficialSystemApplicationsInstallation,
                        "allow_unofficial_system_applications_installation"
                    )
                }

                "com.miui.personalassistant" -> {
                    initHook(ExposureRefreshForNonMIUIWidget, "exposure_refresh_for_non_miui_widget")
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
