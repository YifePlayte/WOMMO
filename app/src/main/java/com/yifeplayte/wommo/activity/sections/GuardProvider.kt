package com.yifeplayte.wommo.activity.sections

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.GuardProviderPushActivity
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.activity.ui.SectionTitle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.ArrowPreference

fun LazyListScope.guardProvider() {
    item {
        SectionTitle(
            titleId = R.string.guard_provider,
            packages = listOf("com.miui.guardprovider"),
        )
    }
    item {
        val context = LocalContext.current
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            SPSwitch(
                key = "block_app_list_upload",
                titleId = R.string.block_app_list_upload,
                summaryId = R.string.block_app_list_upload_tips,
            )
            SPSwitch(
                key = "block_device_state_report",
                titleId = R.string.block_device_state_report,
                summaryId = R.string.block_device_state_report_tips,
            )
            ArrowPreference(
                title = stringResource(R.string.guard_provider_push_page),
                summary = stringResource(R.string.guard_provider_push_page_tips),
                onClick = {
                    context.startActivity(
                        Intent(context, GuardProviderPushActivity::class.java)
                    )
                },
            )
        }
    }
}
