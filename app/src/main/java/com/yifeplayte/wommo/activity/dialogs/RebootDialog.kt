package com.yifeplayte.wommo.activity.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.utils.Terminal
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.LocalDismissState

@Composable
fun RebootDialog(showDialog: MutableState<Boolean>) {
    OverlayDialog(
        title = stringResource(R.string.warning),
        summary = stringResource(R.string.reboot_tips),
        show = showDialog.value,
        onDismissRequest = { showDialog.value = false },
        onDismissFinished = { showDialog.value = false },
    ) {
        val dismissState = LocalDismissState.current
        Row(
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cancel),
                onClick = { dismissState?.invoke() }
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.done),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    Terminal.exec("/system/bin/sync;/system/bin/svc power reboot || reboot")
                    dismissState?.invoke()
                }
            )
        }
    }
}