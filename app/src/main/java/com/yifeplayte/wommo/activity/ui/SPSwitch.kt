package com.yifeplayte.wommo.activity.ui

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.yifeplayte.wommo.utils.SharedPreferences.get
import com.yifeplayte.wommo.utils.SharedPreferences.mSP
import com.yifeplayte.wommo.utils.SharedPreferences.put
import com.yifeplayte.wommo.utils.reloadAllTargets
import top.yukonga.miuix.kmp.preference.SwitchPreference

/** XposedService 绑定状态，通过 CompositionLocal 传递给子组件 */
val LocalServiceReady = compositionLocalOf { false }

@Composable
fun WithServiceReady(serviceReady: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalServiceReady provides serviceReady) {
        content()
    }
}

@Composable
fun SPSwitch(
    key: String,
    title: String? = null,
    summary: String? = null,
    @StringRes titleId: Int? = null,
    @StringRes summaryId: Int? = null,
    defaultValue: Boolean = false,
    enabled: Boolean = true,
    sharedPreferences: SharedPreferences? = mSP,
    switchState: MutableState<Boolean> = remember { mutableStateOf(defaultValue) }
) {
    // service 绑定后立即从 SharedPreferences 刷新开关状态
    val serviceReady = LocalServiceReady.current
    LaunchedEffect(serviceReady) {
        if (serviceReady) {
            mSP?.let { sp -> switchState.value = sp.get(key, defaultValue) }
        }
    }

    val mTitle = title ?: titleId?.let { stringResource(it) } ?: ""
    val mSummary = summary ?: summaryId?.let { stringResource(it) }
    SwitchPreference(
        title = mTitle,
        summary = mSummary,
        checked = switchState.value,
        enabled = enabled,
        onCheckedChange = {
            switchState.value = it
            sharedPreferences.put(key, it)
            reloadAllTargets()
        }
    )
}
