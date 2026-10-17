package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.activity.ui.SectionTitle
import top.yukonga.miuix.kmp.basic.Card

fun LazyListScope.contacts() {
    item {
        SectionTitle(
            titleId = R.string.contacts,
            packages = listOf("com.android.contacts"),
        )
    }
    item {
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            SPSwitch(
                key = "disable_business_hall_offline_info_manager",
                titleId = R.string.disable_business_hall_offline_info_manager,
                summaryId = R.string.disable_business_hall_offline_info_manager_tips,
            )
        }
    }
}
