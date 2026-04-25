package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSwitch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

fun LazyListScope.securityCenter() {
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
            SPSwitch(
                key = "force_support_car_sickness",
                titleId = R.string.force_support_car_sickness,
            )
        }
    }
}
