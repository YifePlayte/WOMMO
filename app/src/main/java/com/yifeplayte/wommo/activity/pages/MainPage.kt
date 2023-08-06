package com.yifeplayte.wommo.activity.pages

import android.annotation.SuppressLint
import android.widget.Toast
import cn.fkj233.ui.activity.annotation.BMMainPage
import cn.fkj233.ui.activity.data.BasePage
import cn.fkj233.ui.activity.view.SeekBarWithTextV
import cn.fkj233.ui.activity.view.SwitchV
import cn.fkj233.ui.activity.view.TextSummaryV
import cn.fkj233.ui.dialog.MIUIDialog
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.hook.PACKAGE_NAME_HOOKED
import com.yifeplayte.wommo.utils.Terminal

@SuppressLint("NonConstantResourceId")
@BMMainPage(titleId = R.string.app_name)
class MainPage : BasePage() {
    override fun onCreate() {
        TitleText(textId = R.string.android)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.force_dark_mode_for_all_apps, tipsId = R.string.force_dark_mode_for_all_apps_tips
            ), SwitchV("force_dark_mode_for_all_apps", false)
        )
        Line()
        TitleText(textId = R.string.system_ui)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.restore_near_by_tile
            ), SwitchV("restore_near_by_tile", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.notification_settings_no_white_list
            ), SwitchV("notification_settings_no_white_list", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.lockscreen_charging_info
            ), SwitchV("lockscreen_charging_info", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.wave_charge
            ), SwitchV("wave_charge", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.redirect_to_notification_channel_setting
            ), SwitchV("redirect_to_notification_channel_setting", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.unlock_control_center_style
            ), SwitchV("unlock_control_center_style", false)
        )
        Line()
        TitleText(textId = R.string.miui_home)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.restore_google_app_icon
            ), SwitchV("restore_google_app_icon", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.restore_switch_minus_screen
            ), SwitchV("restore_switch_minus_screen", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.allow_move_non_miui_widget_to_minus_screen
            ), SwitchV("allow_move_non_miui_widget_to_minus_screen", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.widget_transition_animation
            ), SwitchV("widget_transition_animation", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.fake_non_default_icon
            ), SwitchV("fake_non_default_icon", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.enable_perfect_icons
            ), SwitchV("enable_perfect_icons", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.hide_landscape_nav_bar
            ), SwitchV("hide_landscape_nav_bar", false)
        )
        TextSummaryWithSeekBar(
            TextSummaryV(
                textId = R.string.icon_label_size
            ), SeekBarWithTextV("icon_label_size", 0, 30, 12)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.icon_label_marquee
            ), SwitchV("icon_label_marquee", false)
        )
        Line()
        TitleText(textId = R.string.security_center)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.open_by_default_setting
            ), SwitchV("open_by_default_setting", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.remove_report_in_application_info
            ), SwitchV("remove_report_in_application_info", false)
        )
        Line()
        TitleText(textId = R.string.screen_recorder)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.force_enable_native_playback_capture
            ), SwitchV("force_enable_native_playback_capture", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.change_bitrate_and_frame_rate_range
            ), SwitchV("modify_screen_recorder_config", false)
        )
        Line()
        TitleText(textId = R.string.package_installer)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.allow_unofficial_system_applications_installation
            ), SwitchV("allow_unofficial_system_applications_installation", false)
        )
        Line()
        TitleText(textId = R.string.personal_assistant)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.exposure_refresh_for_non_miui_widget
            ), SwitchV("exposure_refresh_for_non_miui_widget", false)
        )
        Line()
        TitleText(textId = R.string.barrage)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.force_support_barrage
            ), SwitchV("force_support_barrage", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.global_barrage
            ), SwitchV("global_barrage", false)
        )
        TextSummaryWithSeekBar(
            TextSummaryV(
                textId = R.string.barrage_length
            ), SeekBarWithTextV("barrage_length", 20, 120, 36)
        )
        Line()
        TitleText(textId = R.string.settings)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.show_notification_importance
            ), SwitchV("show_notification_importance", false)
        )
        Line()
        TitleText(textId = R.string.download_provider)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.remove_xl_download
            ), SwitchV("remove_xl_download", false)
        )
        Line()
        TitleText(textId = R.string.others)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.force_support_send_app,
                tipsId = R.string.force_support_send_app_tips,
            ), SwitchV("force_support_send_app", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.remove_miui_stroke_from_adaptive_icon,
                tipsId = R.string.remove_miui_stroke_from_adaptive_icon_tips,
            ), SwitchV("remove_miui_stroke_from_adaptive_icon", false)
        )
        Line()
        TitleText(textId = R.string.reboot)
        TextSummaryWithArrow(TextSummaryV(
            textId = R.string.restart_all_scope
        ) {
            MIUIDialog(activity) {
                setTitle(R.string.warning)
                setMessage(R.string.restart_all_scope_tips)
                setLButton(R.string.cancel) {
                    dismiss()
                }
                setRButton(R.string.done) {
                    PACKAGE_NAME_HOOKED.forEach {
                        if (it != "android") Terminal.exec("killall $it")
                    }
                    Toast.makeText(
                        activity, getString(R.string.finished), Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }
            }.show()
        })
        TextSummaryWithArrow(TextSummaryV(
            textId = R.string.reboot_system
        ) {
            MIUIDialog(activity) {
                setTitle(R.string.warning)
                setMessage(R.string.reboot_tips)
                setLButton(R.string.cancel) {
                    dismiss()
                }
                setRButton(R.string.done) {
                    Terminal.exec("/system/bin/sync;/system/bin/svc power reboot || reboot")
                }
            }.show()
        })
    }
}