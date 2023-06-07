package com.yifeplayte.wommo.hook

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow
import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import com.yifeplayte.wommo.hook.hooks.BaseSingleHook
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.AddFreeformShortcut
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.AllowMoveNonMIUIWidgetToMinusScreen
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.RestoreGoogleAppIcon
import com.yifeplayte.wommo.hook.hooks.singlepackage.home.RestoreSwitchMinusScreen
import com.yifeplayte.wommo.hook.hooks.singlepackage.packageinstaller.AllowUnofficialSystemApplicationsInstallation
import com.yifeplayte.wommo.hook.hooks.singlepackage.personalassistant.ExposureRefreshForNonMIUIWidget
import com.yifeplayte.wommo.hook.hooks.singlepackage.screenrecorder.EnablePlaybackCapture
import com.yifeplayte.wommo.hook.hooks.singlepackage.screenrecorder.ModifyScreenRecorderConfig
import com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter.OpenByDefaultSetting
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.LockscreenChargingInfo
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.NotificationSettingsNoWhiteList
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.RestoreNearbyTile
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.WaveCharge
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
            // init DexKit and EzXHelper
            if (lpparam.packageName != "android") DexKit.initDexKit(lpparam)
            EzXHelper.initHandleLoadPackage(lpparam)
            EzXHelper.setLogTag(TAG)
            EzXHelper.setToastTag(TAG)

            // single package
            when (EzXHelper.hostPackageName) {
                "com.miui.screenrecorder" -> {
                    initSingleHook(EnablePlaybackCapture, "force_enable_native_playback_capture")
                    initSingleHook(ModifyScreenRecorderConfig, "modify_screen_recorder_config")
                }

                "com.android.systemui" -> {
                    initSingleHook(RestoreNearbyTile, "restore_near_by_tile")
                    initSingleHook(NotificationSettingsNoWhiteList, "notification_settings_no_white_list")
                    initSingleHook(LockscreenChargingInfo, "lockscreen_charging_info")
                    initSingleHook(WaveCharge, "wave_charge")
                }

                "com.miui.home" -> {
                    initSingleHook(RestoreGoogleAppIcon, "restore_google_app_icon")
                    initSingleHook(AddFreeformShortcut, "add_freeform_shortcut")
                    initSingleHook(RestoreSwitchMinusScreen, "restore_switch_minus_screen")
                    initSingleHook(AllowMoveNonMIUIWidgetToMinusScreen, "allow_move_non_miui_widget_to_minus_screen")
                }

                "com.miui.securitycenter" -> {
                    initSingleHook(OpenByDefaultSetting, "open_by_default_setting")
                }

                "com.miui.packageinstaller" -> {
                    initSingleHook(
                        AllowUnofficialSystemApplicationsInstallation,
                        "allow_unofficial_system_applications_installation"
                    )
                }

                "com.miui.personalassistant" -> {
                    initSingleHook(ExposureRefreshForNonMIUIWidget, "exposure_refresh_for_non_miui_widget")
                }
            }
            DexKit.closeDexKit()
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelper.initZygote(startupParam)
    }

    private fun initSingleHook(hook: BaseSingleHook, key: String, defValue: Boolean = false) =
        initSingleHook(hook, getBoolean(key, defValue))

    private fun initSingleHook(hook: BaseSingleHook, enable: Boolean = true) {
        if (enable) runCatching {
            if (hook.isInit) return
            hook.init()
            hook.isInit = true
            Log.ix("Inited single hook: ${hook.javaClass.simpleName}")
        }.logexIfThrow("Failed init single hook: ${hook.javaClass.simpleName}")
    }

    private fun initMultiHook(hook: BaseMultiHook, key: String, defValue: Boolean = false) =
        initMultiHook(hook, getBoolean(key, defValue))

    private fun initMultiHook(hook: BaseMultiHook, enable: Boolean = true) {
        if (enable) runCatching {
            if (!hook.hooks.containsKey(EzXHelper.hostPackageName)) return
            if (hook.isInit) return
            hook.init()
            hook.isInit = true
            Log.ix("Inited multi hook: ${hook.javaClass.simpleName}")
        }.logexIfThrow("Failed init multi hook: ${hook.javaClass.simpleName}")
    }
}
