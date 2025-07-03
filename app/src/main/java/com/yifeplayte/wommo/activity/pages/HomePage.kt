package com.yifeplayte.wommo.activity.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavController
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.dialogs.NotActivatedDialog
import com.yifeplayte.wommo.activity.dialogs.RebootDialog
import com.yifeplayte.wommo.activity.dialogs.RestartAllScopeDialog
import com.yifeplayte.wommo.activity.ui.SPSlider
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.utils.Build.HYPER_OS_VERSION
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD
import com.yifeplayte.wommo.utils.SharedPreferences.get
import com.yifeplayte.wommo.utils.SharedPreferences.mSP
import com.yifeplayte.wommo.utils.Terminal
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.getWindowSize
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun HomePage(
    navController: NavController? = null,
    currentRoute: String? = null
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.background,
        tint = HazeTint(
            MiuixTheme.colorScheme.background.copy(
                if (scrollBehavior.state.collapsedFraction <= 0f) 1f
                else lerp(1f, 0.67f, (scrollBehavior.state.collapsedFraction))
            )
        )
    )

    val showRebootDialog = remember { mutableStateOf(false) }
    val showRestartAllScopeDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                color = Color.Transparent,
                modifier = Modifier
                    .hazeEffect(hazeState) {
                        style = hazeStyle
                        blurRadius = 25.dp
                        noiseFactor = 0f
                    }
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Left))
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Left))
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                    .windowInsetsPadding(WindowInsets.captionBar.only(WindowInsetsSides.Top)),
                title = stringResource(R.string.app_name),
                scrollBehavior = scrollBehavior,
                defaultWindowInsetsPadding = false
            )
        },
        popupHost = { null }
    ) {
        LazyColumn(
            modifier = Modifier
                .hazeSource(state = hazeState)
                .height(getWindowSize().height.dp)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Left))
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Left)),
            contentPadding = it,
            overscrollEffect = null
        ) {
            item {
                Spacer(Modifier.height(6.dp))
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.android),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    if (!IS_INTERNATIONAL_BUILD) SPSwitch(
                        key = "force_dark_mode_for_all_apps",
                        titleId = R.string.force_dark_mode_for_all_apps,
                        summaryId = R.string.force_dark_mode_for_all_apps_tips,
                    )
                    if (HYPER_OS_VERSION < 2) SPSwitch(
                        key = "use_aosp_share_sheet",
                        titleId = R.string.use_aosp_share_sheet,
                    )
                    SPSwitch(
                        key = "use_aosp_screenshot",
                        titleId = R.string.use_aosp_screenshot,
                    )
                    SPSwitch(
                        key = "disable_safe_media_volume",
                        titleId = R.string.disable_safe_media_volume,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.system_ui),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    if (!IS_INTERNATIONAL_BUILD) SPSwitch(
                        key = "restore_near_by_tile",
                        titleId = R.string.restore_near_by_tile,
                    )
                    if (!IS_INTERNATIONAL_BUILD) SPSwitch(
                        key = "notification_settings_no_white_list",
                        titleId = R.string.notification_settings_no_white_list,
                    )
                    SPSwitch(
                        key = "lockscreen_charging_info",
                        titleId = R.string.lockscreen_charging_info,
                    )
                    SPSwitch(
                        key = "wave_charge",
                        titleId = R.string.wave_charge,
                    )
                    SPSwitch(
                        key = "redirect_to_notification_channel_setting",
                        titleId = R.string.redirect_to_notification_channel_setting,
                    )
                    SPSwitch(
                        key = "unlock_control_center_style",
                        titleId = R.string.unlock_control_center_style,
                    )
                    SPSwitch(
                        key = "restore_hidden_custom_media_action",
                        titleId = R.string.restore_hidden_custom_media_action,
                    )
                    SPSwitch(
                        key = "use_aosp_clipboard_overlay",
                        titleId = R.string.use_aosp_clipboard_overlay,
                    )
                    SPSwitch(
                        key = "hide_mobile_signal_icon",
                        titleId = R.string.hide_mobile_signal_icon,
                    )
                    SPSwitch(
                        key = "hide_bluetooth_icon",
                        titleId = R.string.hide_bluetooth_icon,
                    )
                    SPSwitch(
                        key = "disable_gesture_recorder",
                        titleId = R.string.disable_gesture_recorder,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.miui_home),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    SPSwitch(
                        key = "restore_google_app_icon",
                        titleId = R.string.restore_google_app_icon,
                    )
                    SPSwitch(
                        key = "restore_switch_minus_screen",
                        titleId = R.string.restore_switch_minus_screen,
                    )
                    SPSwitch(
                        key = "allow_move_non_miui_widgets_to_minus_screen",
                        titleId = R.string.allow_move_non_miui_widgets_to_minus_screen,
                    )
                    SPSwitch(
                        key = "show_miui_widgets_in_android_widgets_list",
                        titleId = R.string.show_miui_widgets_in_android_widgets_list,
                    )
                    SPSwitch(
                        key = "widget_transition_animation",
                        titleId = R.string.widget_transition_animation,
                    )
                    SPSwitch(
                        key = "fake_non_default_icon",
                        titleId = R.string.fake_non_default_icon,
                    )
                    SPSwitch(
                        key = "fake_non_disabled_icon",
                        titleId = R.string.fake_non_disabled_icon,
                    )
                    SPSwitch(
                        key = "enable_blur_for_home",
                        titleId = R.string.enable_blur_for_home,
                    )
                    SPSwitch(
                        key = "enable_perfect_icons",
                        titleId = R.string.enable_perfect_icons,
                    )
                    SPSwitch(
                        key = "hide_landscape_nav_bar",
                        titleId = R.string.hide_landscape_nav_bar,
                    )
                    SPSlider(
                        key = "icon_label_size",
                        titleId = R.string.icon_label_size,
                        defaultValue = 37f,
                        minValue = 0f,
                        maxValue = 100f,
                        unit = "px"
                    )
                    SPSwitch(
                        key = "unlock_grids",
                        titleId = R.string.unlock_grids,
                    )
                    SPSwitch(
                        key = "force_applied_light_wallpaper",
                        titleId = R.string.force_applied_light_wallpaper,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.security_center),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    SPSwitch(
                        key = "add_aosp_app_manager_entry",
                        titleId = R.string.add_aosp_app_manager_entry,
                    )
                    SPSwitch(
                        key = "add_aosp_app_info_entry",
                        titleId = R.string.add_aosp_app_info_entry,
                    )
                    SPSwitch(
                        key = "add_open_by_default_entry",
                        titleId = R.string.add_open_by_default_entry,
                    )
                    SPSwitch(
                        key = "remove_report_in_application_info",
                        titleId = R.string.remove_report_in_application_info,
                    )
                    SPSwitch(
                        key = "skip_count_down",
                        titleId = R.string.skip_count_down,
                    )
                    SPSwitch(
                        key = "remove_adb_install_intercept",
                        titleId = R.string.remove_adb_install_intercept,
                    )
                    SPSwitch(
                        key = "prevent_disabling_dev_mode",
                        titleId = R.string.prevent_disabling_dev_mode,
                    )
                    SPSwitch(
                        key = "remove_game_toast",
                        titleId = R.string.remove_game_toast,
                    )
                    SPSwitch(
                        key = "disable_network_assistant_offline_info_manager",
                        titleId = R.string.disable_network_assistant_offline_info_manager,
                        summaryId = R.string.disable_network_assistant_offline_info_manager_tips,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.contacts),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    SPSwitch(
                        key = "disable_business_hall_offline_info_manager",
                        titleId = R.string.disable_business_hall_offline_info_manager,
                        summaryId = R.string.disable_business_hall_offline_info_manager_tips,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.screen_recorder),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    SPSwitch(
                        key = "force_enable_native_playback_capture",
                        titleId = R.string.force_enable_native_playback_capture,
                    )
                    SPSwitch(
                        key = "modify_screen_recorder_config",
                        titleId = R.string.change_bitrate_and_frame_rate_range,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.package_installer),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    SPSwitch(
                        key = "allow_unofficial_system_applications_installation",
                        titleId = R.string.allow_unofficial_system_applications_installation,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.barrage),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    SPSwitch(
                        key = "force_support_barrage",
                        titleId = R.string.force_support_barrage,
                    )
                    SPSwitch(
                        key = "global_barrage",
                        titleId = R.string.global_barrage,
                    )
                    SPSwitch(
                        key = "barrage_not_touchable",
                        titleId = R.string.barrage_not_touchable,
                    )
                    SPSlider(
                        key = "barrage_length",
                        titleId = R.string.barrage_length,
                        defaultValue = 36f,
                        minValue = 20f,
                        maxValue = 200f,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.settings),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    SPSwitch(
                        key = "quick_manage_unknown_app_sources",
                        titleId = R.string.quick_manage_unknown_app_sources,
                    )
                    SPSwitch(
                        key = "quick_manage_overlay_permission",
                        titleId = R.string.quick_manage_overlay_permission,
                    )
                    SPSwitch(
                        key = "show_notification_history_and_log_entry",
                        titleId = R.string.show_notification_history_and_log_entry,
                    )
                    SPSwitch(
                        key = "show_wifi_password",
                        titleId = R.string.show_wifi_password,
                    )
                    if (!IS_INTERNATIONAL_BUILD) SPSwitch(
                        key = "show_google_settings_entry",
                        titleId = R.string.show_google_settings_entry,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.download_provider),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    SPSwitch(
                        key = "remove_xl_download",
                        titleId = R.string.remove_xl_download,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.voice_assist),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    SPSwitch(
                        key = "change_browser_for_mi_ai",
                        titleId = R.string.change_browser_for_mi_ai,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.content_extension),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    SPSwitch(
                        key = "change_browser_for_content_extension",
                        titleId = R.string.change_browser_for_content_extension,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.power_keeper),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    val enableBatteryMonitorService = remember { mutableStateOf(mSP.get("enable_battery_monitor_service", false)) }
                    SPSwitch(
                        key = "enable_battery_monitor_service",
                        titleId = R.string.enable_battery_monitor_service,
                        switchState = enableBatteryMonitorService
                    )
                    AnimatedVisibility(
                        visible = enableBatteryMonitorService.value,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        BasicComponent(
                            title = stringResource(R.string.open_battery_status_activity),
                            onClick = {
                                Terminal.exec("am start -n com.miui.powerkeeper/.ui.powertools.module.batterylife.BatteryStatusActivity")
                            }
                        )
                    }
                }
            }
            if (HYPER_OS_VERSION >= 2) {
                item {
                    SmallTitle(
                        text = stringResource(R.string.intent_resolver),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        SPSwitch(
                            key = "use_aosp_share_sheet",
                            titleId = R.string.use_aosp_share_sheet,
                            summaryId = R.string.use_aosp_share_sheet_tips,
                        )
                    }
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.others),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    SPSwitch(
                        key = "force_support_send_app",
                        titleId = R.string.force_support_send_app,
                        summaryId = R.string.force_support_send_app_tips,
                    )
                    SPSwitch(
                        key = "exposure_refresh_for_non_miui_widget",
                        titleId = R.string.exposure_refresh_for_non_miui_widget,
                        summaryId = R.string.exposure_refresh_for_non_miui_widget_tips,
                    )
                    SPSwitch(
                        key = "show_notification_importance",
                        titleId = R.string.show_notification_importance,
                        summaryId = R.string.show_notification_importance_tips,
                    )
                    SPSwitch(
                        key = "remove_miui_stroke_from_adaptive_icon",
                        titleId = R.string.remove_miui_stroke_from_adaptive_icon,
                        summaryId = R.string.remove_miui_stroke_from_adaptive_icon_tips,
                    )
                }
            }
            item {
                SmallTitle(
                    text = stringResource(R.string.reboot),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    BasicComponent(
                        title = stringResource(R.string.restart_all_scope),
                        titleColor = BasicComponentDefaults.titleColor(
                            color = Color.Red
                        ),
                        onClick = {
                            showRestartAllScopeDialog.value = true
                        }
                    )
                    BasicComponent(
                        title = stringResource(R.string.reboot_system),
                        titleColor = BasicComponentDefaults.titleColor(
                            color = Color.Red
                        ),
                        onClick = {
                            showRebootDialog.value = true
                        }
                    )
                }
            }
            item {
                Spacer(
                    Modifier.height(
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
                    )
                )
            }
        }
    }

    RestartAllScopeDialog(showRestartAllScopeDialog)
    RebootDialog(showRebootDialog)

    if (mSP == null) {
        NotActivatedDialog()
    }
}