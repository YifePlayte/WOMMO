package com.yifeplayte.wommo.activity.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.dialogs.NotActivatedDialog
import com.yifeplayte.wommo.activity.sections.aiEngine
import com.yifeplayte.wommo.activity.sections.android
import com.yifeplayte.wommo.activity.sections.barrage
import com.yifeplayte.wommo.activity.sections.bottomSpacer
import com.yifeplayte.wommo.activity.sections.contacts
import com.yifeplayte.wommo.activity.sections.contentExtension
import com.yifeplayte.wommo.activity.sections.downloadProvider
import com.yifeplayte.wommo.activity.sections.getApps
import com.yifeplayte.wommo.activity.sections.googlePlayServices
import com.yifeplayte.wommo.activity.sections.home
import com.yifeplayte.wommo.activity.sections.intentResolver
import com.yifeplayte.wommo.activity.sections.others
import com.yifeplayte.wommo.activity.sections.packageInstaller
import com.yifeplayte.wommo.activity.sections.powerKeeper
import com.yifeplayte.wommo.activity.sections.reboot
import com.yifeplayte.wommo.activity.sections.screenRecorder
import com.yifeplayte.wommo.activity.sections.securityCenter
import com.yifeplayte.wommo.activity.sections.settings
import com.yifeplayte.wommo.activity.sections.systemUI
import com.yifeplayte.wommo.activity.sections.voiceAssist
import com.yifeplayte.wommo.activity.sections.xmsf
import com.yifeplayte.wommo.activity.ui.AdaptiveTopAppBar
import com.yifeplayte.wommo.activity.ui.BlurredBar
import com.yifeplayte.wommo.utils.SharedPreferences.mSP
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@OptIn(ExperimentalScrollBarApi::class)
@Composable
fun HomePage() {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.app_name),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = if (blurActive) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .fillMaxHeight(),
                contentPadding = innerPadding,
            ) {
                android()
                systemUI()
                home()
                securityCenter()
                contacts()
                screenRecorder()
                packageInstaller()
                barrage()
                settings()
                downloadProvider()
                voiceAssist()
                contentExtension()
                powerKeeper()
                intentResolver()
                aiEngine()
                xmsf()
                getApps()
                googlePlayServices()
                others()
                reboot()
                bottomSpacer()
            }
            VerticalScrollBar(
                adapter = rememberScrollBarAdapter(lazyListState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                trackPadding = innerPadding,
            )
        }
    }


    if (mSP == null) {
        NotActivatedDialog()
    }
}

@Composable
fun rememberBlurBackdrop(): LayerBackdrop? {
    if (!isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}