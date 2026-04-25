package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.utils.Build.HYPER_OS_VERSION
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

fun LazyListScope.android() {
    item {
        SmallTitle(
            text = stringResource(R.string.android),
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
            if (!IS_INTERNATIONAL_BUILD) SPSwitch(
                key = "force_dark_mode_for_all_apps",
                titleId = R.string.force_dark_mode_for_all_apps,
                summaryId = R.string.force_dark_mode_for_all_apps_tips,
            )
            if (HYPER_OS_VERSION < 2) SPSwitch(
                key = "use_aosp_share_sheet",
                titleId = R.string.use_aosp_share_sheet,
            )
            SPSwitch(
                key = "use_aosp_screenshot",
                titleId = R.string.use_aosp_screenshot,
            )
            SPSwitch(
                key = "disable_safe_media_volume",
                titleId = R.string.disable_safe_media_volume,
            )
        }
    }
}
