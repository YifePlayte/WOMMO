package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.activity.ui.SectionTitle
import top.yukonga.miuix.kmp.basic.Card

fun LazyListScope.personalAssistant() {
    item {
        SectionTitle(
            titleId = R.string.personal_assistant,
            packages = listOf("com.miui.personalassistant"),
        )
    }
    item {
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            SPSwitch(
                key = "no_stack_swipe_background",
                titleId = R.string.no_stack_swipe_background,
            )
        }
    }
}
