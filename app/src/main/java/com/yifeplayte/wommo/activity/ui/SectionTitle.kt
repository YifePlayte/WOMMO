package com.yifeplayte.wommo.activity.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.hook.utils.SYSTEM_SERVER_PACKAGE_NAME
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 分区分头请求重启的回调
 *
 * 由页面持有对话框状态，分区自己只发起请求（弹窗跟随列表项一起滚动会被回收）。
 */
val LocalRestartRequest = compositionLocalOf<(List<String>) -> Unit> { {} }

/**
 * 分区分头：标题 + 该分区对应应用的重启按钮
 *
 * 按钮 killall 列出的包，让该分区的 hook 重新加载。system_server 不参与：
 * 它靠模块热重载生效，且进程名与一个无关的 MIUI daemon 撞名，误杀会连带重启它；
 * 因此只以 system_server 为目标的分区不会出现按钮。
 */
@Composable
fun SectionTitle(@StringRes titleId: Int, packages: List<String>) {
    val requestRestart = LocalRestartRequest.current
    val targets = remember(packages) {
        packages.filter { it != SYSTEM_SERVER_PACKAGE_NAME }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallTitle(
            text = stringResource(titleId),
            modifier = Modifier.weight(1f),
            textColor = MiuixTheme.colorScheme.onBackgroundVariant,
        )
        if (targets.isNotEmpty()) {
            IconButton(onClick = { requestRestart(targets) }) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = stringResource(R.string.restart),
                    tint = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }
    }
}
