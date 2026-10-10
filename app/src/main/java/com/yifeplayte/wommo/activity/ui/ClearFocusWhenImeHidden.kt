package com.yifeplayte.wommo.activity.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager

/**
 * 收起输入法时清掉焦点，否则光标会留在输入框里，键盘再弹出时又直接续上。
 *
 * 只能在「输入法曾经显示过、现在又消失」这个跃迁上清：从点击到输入法弹出之间还有
 * 几帧 [isImeVisible] 是 false，那段时间误清会把用户的点击吃掉，光标刚出现就被收走。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClearFocusWhenImeHidden() {
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeWasVisible && !imeVisible) focusManager.clearFocus()
        imeWasVisible = imeVisible
    }
}