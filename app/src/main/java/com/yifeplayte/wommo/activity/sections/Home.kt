package com.yifeplayte.wommo.activity.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSlider
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.activity.ui.SectionTitle
import com.yifeplayte.wommo.utils.Build.HYPER_OS_VERSION
import top.yukonga.miuix.kmp.basic.Card

fun LazyListScope.home() {
    // HyperOS 4 and newer ship the Flutter launcher, which owns no launcher dex
    // at all, so only the switches with a native implementation do anything
    // there.  The Java-only switches (and the native-only ones on the older
    // launcher) would be dead rows, so each is shown for the launcher it serves.
    val nativeLauncher = HYPER_OS_VERSION >= 4
    item {
        SectionTitle(
            titleId = R.string.miui_home,
            packages = listOf("com.miui.home"),
        )
    }
    item {
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            if (!nativeLauncher) SPSwitch(
                key = "restore_google_app_icon",
                titleId = R.string.restore_google_app_icon,
            )
            if (!nativeLauncher) SPSwitch(
                key = "restore_switch_minus_screen",
                titleId = R.string.restore_switch_minus_screen,
            )
            SPSwitch(
                key = "allow_move_non_miui_widgets_to_minus_screen",
                titleId = R.string.allow_move_non_miui_widgets_to_minus_screen,
            )
            if (!nativeLauncher) SPSwitch(
                key = "show_miui_widgets_in_android_widgets_list",
                titleId = R.string.show_miui_widgets_in_android_widgets_list,
            )
            if (!nativeLauncher) SPSwitch(
                key = "widget_transition_animation",
                titleId = R.string.widget_transition_animation,
            )
            if (nativeLauncher) SPSwitch(
                key = "restore_back_home_ratio",
                titleId = R.string.restore_back_home_ratio,
                summaryId = R.string.restore_back_home_ratio_tips,
            )
            if (!nativeLauncher) SPSwitch(
                key = "fake_non_default_icon",
                titleId = R.string.fake_non_default_icon,
            )
            if (!nativeLauncher) SPSwitch(
                key = "fake_non_disabled_icon",
                titleId = R.string.fake_non_disabled_icon,
            )
            if (!nativeLauncher) SPSwitch(
                key = "enable_blur_for_home",
                titleId = R.string.enable_blur_for_home,
            )
            SPSwitch(
                key = "enable_perfect_icons",
                titleId = R.string.enable_perfect_icons,
            )
            if (nativeLauncher) SPSwitch(
                key = "disable_icon_highlight",
                titleId = R.string.disable_icon_highlight,
                summaryId = R.string.disable_icon_highlight_tips,
            )
            SPSwitch(
                key = "hide_landscape_nav_bar",
                titleId = R.string.hide_landscape_nav_bar,
            )
            if (!nativeLauncher) SPSlider(
                key = "icon_label_size",
                titleId = R.string.icon_label_size,
                defaultValue = 37f,
                minValue = 0f,
                maxValue = 100f,
                unit = "px"
            )
            if (!nativeLauncher) SPSwitch(
                key = "unlock_grids",
                titleId = R.string.unlock_grids,
            )
            if (nativeLauncher) SPSwitch(
                key = "allow_search_on_4x7",
                titleId = R.string.allow_search_on_4x7,
            )
            if (!nativeLauncher) SPSwitch(
                key = "force_applied_light_wallpaper",
                titleId = R.string.force_applied_light_wallpaper,
            )
        }
    }
}
