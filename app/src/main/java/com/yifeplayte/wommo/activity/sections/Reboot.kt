package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.dialogs.RebootDialog
import com.yifeplayte.wommo.activity.dialogs.RestartAllScopeDialog
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

fun LazyListScope.reboot() {
    item {
        SmallTitle(
            text = stringResource(R.string.reboot),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    item {
        val showRebootDialog = remember { mutableStateOf(false) }
        val showRestartAllScopeDialog = remember { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            BasicComponent(
                title = stringResource(R.string.restart_all_scope),
                titleColor = BasicComponentDefaults.titleColor(
                    color = Color.Red
                ),
                onClick = {
                    showRestartAllScopeDialog.value = true
                }
            )
            BasicComponent(
                title = stringResource(R.string.reboot_system),
                titleColor = BasicComponentDefaults.titleColor(
                    color = Color.Red
                ),
                onClick = {
                    showRebootDialog.value = true
                }
            )
        }
        RestartAllScopeDialog(showRestartAllScopeDialog)
        RebootDialog(showRebootDialog)
    }
}
