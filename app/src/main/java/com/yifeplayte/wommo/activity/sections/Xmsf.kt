package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.activity.ui.SectionTitle
import top.yukonga.miuix.kmp.basic.Card

fun LazyListScope.xmsf() {
    item {
        SectionTitle(
            titleId = R.string.xmsf,
            packages = listOf("com.xiaomi.xmsf"),
        )
    }
    item {
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            SPSwitch(
                key = "force_auth_success_for_xmsf",
                titleId = R.string.force_auth_success_for_xmsf,
                summaryId = R.string.force_auth_success_for_xmsf_tips,
            )
        }
    }
}
