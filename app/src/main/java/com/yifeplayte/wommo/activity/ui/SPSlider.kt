package com.yifeplayte.wommo.activity.ui

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.utils.SharedPreferences.get
import com.yifeplayte.wommo.utils.SharedPreferences.mSP
import com.yifeplayte.wommo.utils.SharedPreferences.put
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun SPSlider(
    key: String,
    title: String? = null,
    summary: String? = null,
    @StringRes titleId: Int? = null,
    @StringRes summaryId: Int? = null,
    defaultValue: Float,
    minValue: Float,
    maxValue: Float,
    decimalPlaces: Int = 0,
    unit: String = "",
    enabled: Boolean = true,
    sharedPreferences: SharedPreferences? = mSP,
    titleColor: SPSliderColors = SPSliderDefaults.titleColor(),
    summaryColor: SPSliderColors = SPSliderDefaults.summaryColor(),
    insideMargin: PaddingValues = SPSliderDefaults.InsideMargin,
    progress: MutableFloatState = remember { mutableFloatStateOf(sharedPreferences.get(key, defaultValue).coerceIn(minValue, maxValue)) },
) {
    val mTitle = title ?: titleId?.let { stringResource(it) } ?: ""
    val mSummary = summary ?: summaryId?.let { stringResource(it) }

    val interactionSource = remember { MutableInteractionSource() }
    val valueColor = if (enabled) colorScheme.onSurfaceVariantSummary else colorScheme.disabledOnSecondaryVariant

    Column(
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth()
            .indication(interactionSource, LocalIndication.current)
            .padding(insideMargin)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = mTitle,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = titleColor.color(enabled)
                )
                mSummary?.let {
                    Text(
                        text = it,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = summaryColor.color(enabled)
                    )
                }
            }
            Text(
                modifier = Modifier,
                color = valueColor,
                text = if (progress.floatValue == defaultValue) stringResource(R.string.default_value)
                else if (decimalPlaces == 0) progress.floatValue.coerceIn(minValue, maxValue).toInt().toString() + unit
                else progress.floatValue.coerceIn(minValue, maxValue).toString() + unit,
                textAlign = TextAlign.End,
                fontSize = 14.sp
            )
        }
        Slider(
            progress = progress.floatValue,
            onProgressChange = { newProgress ->
                progress.floatValue = newProgress
                sharedPreferences.put(key, progress.floatValue)
            },
            maxValue = maxValue,
            minValue = minValue,
            decimalPlaces = decimalPlaces,
            modifier = Modifier
                .padding(top = 10.dp),
            enabled = enabled,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        )
    }

}


object SPSliderDefaults {

    /**
     * The default margin inside the [SPSlider].
     */
    val InsideMargin = PaddingValues(16.dp)

    /**
     * The default color of the title.
     */
    @Composable
    fun titleColor(
        color: Color = colorScheme.onSurface,
        disabledColor: Color = colorScheme.disabledOnSecondaryVariant
    ): SPSliderColors {
        return SPSliderColors(
            color = color,
            disabledColor = disabledColor
        )
    }

    /**
     * The default color of the summary.
     */
    @Composable
    fun summaryColor(
        color: Color = colorScheme.onSurfaceVariantSummary,
        disabledColor: Color = colorScheme.disabledOnSecondaryVariant
    ): SPSliderColors = SPSliderColors(
        color = color,
        disabledColor = disabledColor
    )
}

@Immutable
class SPSliderColors(
    private val color: Color,
    private val disabledColor: Color
) {
    @Stable
    internal fun color(enabled: Boolean): Color = if (enabled) color else disabledColor
}