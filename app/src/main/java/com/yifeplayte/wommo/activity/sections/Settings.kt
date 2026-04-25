package com.yifeplayte.wommo.activity.sections

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

fun LazyListScope.settings() {
    item {
        SmallTitle(
            text = stringResource(R.string.settings),
        )
    }
    item {
        Card(
            modifier = Modifier
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
}
