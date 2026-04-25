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

fun LazyListScope.voiceAssist() {
    item {
        SmallTitle(
            text = stringResource(R.string.voice_assist),
        )
    }
    item {
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            SPSwitch(
                key = "change_browser_for_mi_ai",
                titleId = R.string.change_browser_for_mi_ai,
            )
            SPSwitch(
                key = "enable_wake_up_advanced_animation",
                titleId = R.string.enable_wake_up_advanced_animation,
            )
        }
    }
}
