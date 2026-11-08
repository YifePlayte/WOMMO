package com.yifeplayte.wommo.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.App
import com.yifeplayte.wommo.activity.pages.rememberBlurBackdrop
import com.yifeplayte.wommo.activity.sections.bottomSpacer
import com.yifeplayte.wommo.activity.ui.BlurredBar
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.activity.ui.WithServiceReady
import com.yifeplayte.wommo.activity.pages.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

class GuardProviderPushActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiuixTheme(
                colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                GuardProviderPushScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
private fun GuardProviderPushScreen(onClose: () -> Unit) {
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafeInsets = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal)
        .asPaddingValues()

    // 与首页一致：监听 XposedService 绑定状态，SPSwitch 依赖它读取已保存的值
    var serviceReady by remember { mutableStateOf(App.mService != null) }
    DisposableEffect(Unit) {
        val listener = object : App.ServiceStateListener {
            override fun onServiceStateChanged(service: io.github.libxposed.service.XposedService?) {
                serviceReady = service != null
            }
        }
        App.addServiceStateListener(listener, notifyImmediately = true)
        onDispose { App.removeServiceStateListener(listener) }
    }


    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive) {
                TopAppBar(
                    title = stringResource(R.string.guard_provider_push_page),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Back,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        WithServiceReady(serviceReady) {
            Box(
                modifier = if (blurActive) {
                    Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop)
                } else {
                    Modifier.fillMaxSize()
                }
            ) {
                LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .consumeWindowInsets(paddingValues)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = horizontalSafeInsets.calculateStartPadding(layoutDirection),
                    top = paddingValues.calculateTopPadding(),
                    end = horizontalSafeInsets.calculateEndPadding(layoutDirection),
                    bottom = paddingValues.calculateBottomPadding(),
                ),
                overscrollEffect = null,
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        SPSwitch(
                            key = "block_push_command_collect_package",
                            titleId = R.string.block_push_command_collect_package,
                            summaryId = R.string.block_push_command_collect_package_tips,
                        )
                        SPSwitch(
                            key = "block_push_command_disable_package",
                            titleId = R.string.block_push_command_disable_package,
                            summaryId = R.string.block_push_command_disable_package_tips,
                        )
                        SPSwitch(
                            key = "block_push_command_enable_package",
                            titleId = R.string.block_push_command_enable_package,
                            summaryId = R.string.block_push_command_enable_package_tips,
                        )
                        SPSwitch(
                            key = "block_push_command_disable_component",
                            titleId = R.string.block_push_command_disable_component,
                            summaryId = R.string.block_push_command_disable_component_tips,
                        )
                        SPSwitch(
                            key = "block_push_command_enable_component",
                            titleId = R.string.block_push_command_enable_component,
                            summaryId = R.string.block_push_command_enable_component_tips,
                        )
                        SPSwitch(
                            key = "block_push_command_clear_a11y",
                            titleId = R.string.block_push_command_clear_a11y,
                            summaryId = R.string.block_push_command_clear_a11y_tips,
                        )
                        SPSwitch(
                            key = "block_push_command_scan_and_process",
                            titleId = R.string.block_push_command_scan_and_process,
                            summaryId = R.string.block_push_command_scan_and_process_tips,
                        )
                        SPSwitch(
                            key = "block_push_command_set_prop",
                            titleId = R.string.block_push_command_set_prop,
                            summaryId = R.string.block_push_command_set_prop_tips,
                        )
                        SPSwitch(
                            key = "block_push_command_set_settings",
                            titleId = R.string.block_push_command_set_settings,
                            summaryId = R.string.block_push_command_set_settings_tips,
                        )
                        SPSwitch(
                            key = "block_push_command_update_virus_db",
                            titleId = R.string.block_push_command_update_virus_db,
                            summaryId = R.string.block_push_command_update_virus_db_tips,
                        )
                    }
                }
                bottomSpacer()
                }
            }
        }
    }
}
