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
import com.yifeplayte.wommo.utils.Terminal
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.LocalDismissState

/**
 * 只有一个分区对应应用的重启确认弹窗
 *
 * 与“重启全部作用域”同规则：killall 后由系统重新拉起进程，system_server 不在列表里
 * （见 SectionTitle）。
 */
@Composable
fun RestartScopeDialog(targets: List<String>, showDialog: MutableState<Boolean>) {
    if (targets.isEmpty()) return
    OverlayDialog(
        title = stringResource(R.string.warning),
        summary = stringResource(R.string.restart_scope_tips, targets.joinToString("\n")),
        show = showDialog.value,
        onDismissRequest = { showDialog.value = false },
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
                    targets.forEach { Terminal.exec("killall $it") }
                    Toast.makeText(
                        appContext, appContext.getString(R.string.finished), Toast.LENGTH_SHORT
                    ).show()
                    dismissState?.invoke()
                }
            )
        }
    }
}
