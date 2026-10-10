package com.yifeplayte.wommo.activity.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.yifeplayte.wommo.utils.SharedPreferences.get
import com.yifeplayte.wommo.utils.SharedPreferences.mSP
import com.yifeplayte.wommo.utils.SharedPreferences.put
import com.yifeplayte.wommo.utils.reloadAllTargets
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.TextField

/**
 * Text input bound to a String entry of the module preferences.
 *
 * Hooks only read their configuration at process start, so a change has to
 * reload the targets. Reloading on every keystroke would restart all hooked
 * processes continuously, therefore the reload is debounced while the value
 * itself is persisted immediately (so nothing is lost if the module is reloaded
 * or the process dies before the debounce fires).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SPTextField(
    key: String,
    @StringRes labelId: Int? = null,
    label: String? = null,
    defaultValue: String = "",
    maxLines: Int = 1,
    modifier: Modifier = Modifier,
) {
    val serviceReady = LocalServiceReady.current
    val focusManager = LocalFocusManager.current
    var text by remember { mutableStateOf(defaultValue) }
    var dirty by remember { mutableStateOf(false) }

    ClearFocusWhenImeHidden()

    // 绑定服务后才读取已保存的值，避免先用默认值覆盖用户配置
    LaunchedEffect(serviceReady) {
        if (serviceReady && !dirty) {
            mSP?.let { text = it.get(key, defaultValue) }
        }
    }

    LaunchedEffect(text) {
        if (!dirty) return@LaunchedEffect
        delay(1500)
        dirty = false
        reloadAllTargets()
    }

    val singleLine = maxLines == 1
    TextField(
        label = label ?: labelId?.let { stringResource(it) } ?: "",
        modifier = modifier,
        value = text,
        maxLines = maxLines,
        onValueChange = {
            text = it
            dirty = true
            mSP?.put(key, it)
        },
        // 单行按「完成」收起键盘并失焦；多行要能换行，所以用默认的换行动作
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    )
}