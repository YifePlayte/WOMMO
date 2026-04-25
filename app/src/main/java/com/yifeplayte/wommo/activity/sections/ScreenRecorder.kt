package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSwitch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

fun LazyListScope.screenRecorder() {
    item {
        SmallTitle(
            text = stringResource(R.string.screen_recorder),
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
