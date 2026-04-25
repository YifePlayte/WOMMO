package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

fun LazyListScope.systemUI() {
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
                key = "show_bluetooth_handsfree_battery_icon",
                titleId = R.string.show_bluetooth_handsfree_battery_icon,
            )
            SPSwitch(
                key = "disable_gesture_recorder",
                titleId = R.string.disable_gesture_recorder,
            )
            SPSwitch(
                key = "hide_navigation_bar",
                titleId = R.string.hide_navigation_bar,
            )
        }
    }
}
