package com.yifeplayte.wommo.activity.pages

import android.annotation.SuppressLint
import android.widget.Toast
import cn.fkj233.ui.activity.annotation.BMMainPage
import cn.fkj233.ui.activity.data.BasePage
import cn.fkj233.ui.activity.view.SwitchV
import cn.fkj233.ui.activity.view.TextSummaryV
import cn.fkj233.ui.dialog.MIUIDialog
import com.yifeplayte.maxfreeform.utils.Terminal
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.hook.PACKAGE_NAME_HOOKED

@SuppressLint("NonConstantResourceId")
@BMMainPage(titleId = R.string.app_name)
class MainPage : BasePage() {
    override fun onCreate() {
        TitleText(textId = R.string.miui_home)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.restore_google_app_icon
            ),
            SwitchV("restore_google_app_icon", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.add_freeform_shortcut
            ),
            SwitchV("add_freeform_shortcut", false)
        )

        TitleText(textId = R.string.system_ui)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.restore_near_by_tile
            ),
            SwitchV("restore_near_by_tile", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.notification_settings_no_white_list
            ),
            SwitchV("notification_settings_no_white_list", false)
        )

        TitleText(textId = R.string.security_center)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.open_by_default_setting
            ),
            SwitchV("open_by_default_setting", false)
        )

        TitleText(textId = R.string.screen_recorder)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.force_enable_native_playback_capture
            ),
            SwitchV("force_enable_native_playback_capture", false)
        )
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.change_bitrate_and_frame_rate_range
            ),
            SwitchV("modify_screen_recorder_config", false)
        )

        TitleText(textId = R.string.package_installer)
        TextSummaryWithSwitch(
            TextSummaryV(
                textId = R.string.allow_unofficial_system_applications_installation
            ),
            SwitchV("allow_unofficial_system_applications_installation", false)
        )

        TitleText(textId = R.string.reboot)
        TextSummaryWithArrow(
            TextSummaryV(
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
                            activity,
                            getString(R.string.finished),
                            Toast.LENGTH_SHORT
                        ).show()
                        dismiss()
                    }
                }.show()
            }
        )
        TextSummaryWithArrow(
            TextSummaryV(
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
            }
        )
    }
}