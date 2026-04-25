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

fun LazyListScope.others() {
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
}
