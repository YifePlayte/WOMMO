package com.yifeplayte.wommo.activity.ui

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.utils.SharedPreferences.get
import com.yifeplayte.wommo.utils.SharedPreferences.mSP
import com.yifeplayte.wommo.utils.SharedPreferences.put
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun SPSlider(
    key: String,
    title: String? = null,
    @StringRes titleId: Int? = null,
    defaultValue: Float,
    minValue: Float,
    maxValue: Float,
    unit: String = "",
    enabled: Boolean = true,
    isInt: Boolean = true,
    sharedPreferences: SharedPreferences? = mSP,
    progress: MutableFloatState = remember { mutableFloatStateOf(sharedPreferences.get(key, defaultValue).coerceIn(minValue, maxValue)) },
) {
    val mTitle = title ?: titleId?.let { stringResource(it) } ?: ""
    val showSliderDialog = remember { mutableStateOf(false) }
    val sliderDialogHoldDown = remember { mutableStateOf(false) }

    ArrowPreference(
        title = mTitle,
        enabled = enabled,
        endActions = {
            Text(
                text = (if (progress.floatValue == defaultValue) stringResource(R.string.default_value) + " " else "") +
                        (if (isInt) progress.floatValue.coerceIn(minValue, maxValue).toInt().toString() + unit
                        else progress.floatValue.coerceIn(minValue, maxValue).toString() + unit),
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = {
            showSliderDialog.value = true
            sliderDialogHoldDown.value = true
        },
        holdDownState = sliderDialogHoldDown.value,
        bottomAction = {
            Slider(
                value = progress.floatValue,
                onValueChange = { newProgress ->
                    progress.floatValue = newProgress
                    sharedPreferences.put(key, progress.floatValue)
                },
                valueRange = minValue..maxValue,
                steps = if (isInt) maxValue.toInt() - minValue.toInt() - 1 else 0,
                showKeyPoints = true,
                enabled = enabled,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                keyPoints = listOf(defaultValue),
            )
        },
    )

    SliderDialog(
        showSliderDialog,
        valueState = { progress.floatValue },
        onValueChange = {
            progress.floatValue = it
            sharedPreferences.put(key, progress.floatValue)
        },
        onDismissFinished = { sliderDialogHoldDown.value = false },
        title = mTitle,
        valueRange = minValue..maxValue,
        isInt = isInt,
        defaultValue = defaultValue,
    )
}

@Composable
private fun SliderDialog(
    showDialog: MutableState<Boolean>,
    valueState: () -> Float,
    onValueChange: (Float) -> Unit,
    onDismissFinished: () -> Unit,
    title: String,
    valueRange: ClosedFloatingPointRange<Float>,
    isInt: Boolean,
    defaultValue: Float,
) {
    WindowDialog(
        show = showDialog.value,
        title = title,
        summary = if (isInt) valueRange.start.toInt().toString() + " - " + valueRange.endInclusive.toInt().toString()
        else valueRange.start.toString() + " - " + valueRange.endInclusive.toString(),
        onDismissRequest = {
            showDialog.value = false
        },
        onDismissFinished = onDismissFinished,
        content = {
            var text by remember { mutableStateOf((if (isInt) valueState().toInt() else valueState()).toString()) }
            TextField(
                label = stringResource(R.string.empty_to_default),
                modifier = Modifier.padding(bottom = 16.dp),
                value = text,
                maxLines = 1,
                onValueChange = { newValue ->
                    val digits = newValue.filter { it.isDigit() }
                    if (digits.isEmpty()) {
                        text = ""
                    } else {
                        val limited = digits.take(3)
                        val num = limited.toFloatOrNull() ?: 0f
                        val clamped = num.coerceIn(valueRange)
                        text = (if (isInt) clamped.toInt() else clamped).toString()
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = { showDialog.value = false },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(R.string.done),
                    onClick = {
                        val parsed = text.toFloatOrNull()
                        val clamped = parsed?.coerceIn(valueRange) ?: defaultValue
                        onValueChange(clamped)
                        showDialog.value = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}
