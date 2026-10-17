package com.yifeplayte.wommo.activity.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.activity.ui.SPTextField
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

fun LazyListScope.others() {
    item {
        SmallTitle(
            text = stringResource(R.string.others),
        )
    }
    item {
        Card(
            modifier = Modifier
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
                key = "disable_display_content_mode_management",
                titleId = R.string.disable_display_content_mode_management,
                summaryId = R.string.disable_display_content_mode_management_tips,
            )
            val aiSummaryEnabled = remember { mutableStateOf(false) }
            SPSwitch(
                key = "allow_extra_ai_summary_apps",
                titleId = R.string.allow_extra_ai_summary_apps,
                summaryId = R.string.allow_extra_ai_summary_apps_tips,
                switchState = aiSummaryEnabled,
            )
            // 规则输入框只在开关打开时显示，展开用 fade + expand；左右留白与偏好项的
            // insideMargin（16.dp）对齐，顶部不空出来让它贴住上方开关行
            AnimatedVisibility(
                visible = aiSummaryEnabled.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                SPTextField(
                    key = "ai_summary_extra_rules",
                    labelId = R.string.ai_summary_extra_rules,
                    maxLines = 6,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
            }
            SPSwitch(
                key = "joystick_touch_guard",
                titleId = R.string.joystick_touch_guard,
                summaryId = R.string.joystick_touch_guard_tips,
            )
        }
    }
}
