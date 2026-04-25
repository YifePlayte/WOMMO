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

fun LazyListScope.home() {
    item {
        SmallTitle(
            text = stringResource(R.string.miui_home),
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
                key = "restore_google_app_icon",
                titleId = R.string.restore_google_app_icon,
            )
            SPSwitch(
                key = "restore_switch_minus_screen",
                titleId = R.string.restore_switch_minus_screen,
            )
            SPSwitch(
                key = "allow_move_non_miui_widgets_to_minus_screen",
                titleId = R.string.allow_move_non_miui_widgets_to_minus_screen,
            )
            SPSwitch(
                key = "show_miui_widgets_in_android_widgets_list",
                titleId = R.string.show_miui_widgets_in_android_widgets_list,
            )
            SPSwitch(
                key = "widget_transition_animation",
                titleId = R.string.widget_transition_animation,
            )
            SPSwitch(
                key = "fake_non_default_icon",
                titleId = R.string.fake_non_default_icon,
            )
            SPSwitch(
                key = "fake_non_disabled_icon",
                titleId = R.string.fake_non_disabled_icon,
            )
            SPSwitch(
                key = "enable_blur_for_home",
                titleId = R.string.enable_blur_for_home,
            )
            SPSwitch(
                key = "enable_perfect_icons",
                titleId = R.string.enable_perfect_icons,
            )
            SPSwitch(
                key = "hide_landscape_nav_bar",
                titleId = R.string.hide_landscape_nav_bar,
            )
            SPSlider(
                key = "icon_label_size",
                titleId = R.string.icon_label_size,
                defaultValue = 37f,
                minValue = 0f,
                maxValue = 100f,
                unit = "px"
            )
            SPSwitch(
                key = "unlock_grids",
                titleId = R.string.unlock_grids,
            )
            SPSwitch(
                key = "force_applied_light_wallpaper",
                titleId = R.string.force_applied_light_wallpaper,
            )
        }
    }
}
