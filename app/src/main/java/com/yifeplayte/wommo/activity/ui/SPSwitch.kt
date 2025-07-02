package com.yifeplayte.wommo.activity.ui

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.yifeplayte.wommo.utils.SharedPreferences.get
import com.yifeplayte.wommo.utils.SharedPreferences.mSP
import com.yifeplayte.wommo.utils.SharedPreferences.put
import top.yukonga.miuix.kmp.extra.SuperSwitch

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
    switchState: MutableState<Boolean> = remember { mutableStateOf(sharedPreferences.get(key, defaultValue)) }
) {
    val mTitle = title ?: titleId?.let { stringResource(it) } ?: ""
    val mSummary = summary ?: summaryId?.let { stringResource(it) }
    SuperSwitch(
        title = mTitle,
        summary = mSummary,
        checked = switchState.value,
        enabled = enabled,
        onCheckedChange = {
            switchState.value = it
            sharedPreferences.put(key, it)
        }
    )
}