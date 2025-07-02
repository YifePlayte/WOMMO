package com.yifeplayte.wommo.activity.dialogs

import android.widget.Toast
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
import com.yifeplayte.wommo.activity.MainActivity.Companion.appContext
import com.yifeplayte.wommo.hook.PACKAGE_NAME_HOOKED
import com.yifeplayte.wommo.utils.Terminal
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog

@Composable
fun RestartAllScopeDialog(showDialog: MutableState<Boolean>) {
    SuperDialog(
        title = stringResource(R.string.warning),
        summary = stringResource(R.string.restart_all_scope_tips),
        show = showDialog,
        onDismissRequest = { showDialog.value = false },
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cancel),
                onClick = { showDialog.value = false }
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.done),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    PACKAGE_NAME_HOOKED.forEach {
                        if (it != "android") Terminal.exec("killall $it")
                    }
                    Toast.makeText(
                        appContext, appContext.getString(R.string.finished), Toast.LENGTH_SHORT
                    ).show()
                    showDialog.value = false
                }
            )
        }
    }
}