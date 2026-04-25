package com.yifeplayte.wommo.activity.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.activity.ui.SPSwitch
import com.yifeplayte.wommo.utils.SharedPreferences.get
import com.yifeplayte.wommo.utils.SharedPreferences.mSP
import com.yifeplayte.wommo.utils.Terminal
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle

fun LazyListScope.powerKeeper() {
    item {
        SmallTitle(
            text = stringResource(R.string.power_keeper),
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
            val enableBatteryMonitorService = remember { mutableStateOf(mSP.get("enable_battery_monitor_service", false)) }
            SPSwitch(
                key = "enable_battery_monitor_service",
                titleId = R.string.enable_battery_monitor_service,
                switchState = enableBatteryMonitorService
            )
            AnimatedVisibility(
                visible = enableBatteryMonitorService.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                BasicComponent(
                    title = stringResource(R.string.open_battery_status_activity),
                    onClick = {
                        Terminal.exec("am start -n com.miui.powerkeeper/.ui.powertools.module.batterylife.BatteryStatusActivity")
                    }
                )
            }
        }
    }
}
