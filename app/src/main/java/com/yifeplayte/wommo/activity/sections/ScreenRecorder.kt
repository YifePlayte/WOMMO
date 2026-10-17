package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.activity.ui.SectionTitle
import top.yukonga.miuix.kmp.basic.Card

fun LazyListScope.screenRecorder() {
    item {
        SectionTitle(
            titleId = R.string.screen_recorder,
            packages = listOf("com.miui.screenrecorder"),
        )
    }
    item {
        Card(
            modifier = Modifier
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
}
