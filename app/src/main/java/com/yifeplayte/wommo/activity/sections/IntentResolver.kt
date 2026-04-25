package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.utils.Build.HYPER_OS_VERSION
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

fun LazyListScope.intentResolver() {
    if (HYPER_OS_VERSION >= 2) {
        item {
            SmallTitle(
                text = stringResource(R.string.intent_resolver),
            )
        }
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
            ) {
                SPSwitch(
                    key = "use_aosp_share_sheet",
                    titleId = R.string.use_aosp_share_sheet,
                    summaryId = R.string.use_aosp_share_sheet_tips,
                )
            }
        }
    }
}
