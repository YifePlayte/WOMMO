package com.yifeplayte.wommo.activity.dialogs

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat.finishAffinity
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.MainActivity.Companion.appContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import kotlin.system.exitProcess

@Composable
fun NotActivatedDialog() {
    OverlayDialog(
        title = stringResource(R.string.warning),
        summary = stringResource(R.string.not_support),
        show = true
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.done),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    finishAffinity(appContext as Activity)
                    exitProcess(0)
                }
            )
        }
    }
}