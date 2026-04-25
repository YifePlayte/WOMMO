package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSlider
import com.yifeplayte.wommo.activity.ui.SPSwitch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

fun LazyListScope.barrage() {
    item {
        SmallTitle(
            text = stringResource(R.string.barrage),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    item {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            SPSwitch(
                key = "force_support_barrage",
                titleId = R.string.force_support_barrage,
            )
            SPSwitch(
                key = "global_barrage",
                titleId = R.string.global_barrage,
            )
            SPSwitch(
                key = "barrage_not_touchable",
                titleId = R.string.barrage_not_touchable,
            )
            SPSlider(
                key = "barrage_length",
                titleId = R.string.barrage_length,
                defaultValue = 36f,
                minValue = 20f,
                maxValue = 200f,
            )
        }
    }
}
