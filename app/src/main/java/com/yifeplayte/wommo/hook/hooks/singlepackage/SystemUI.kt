package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.LockscreenChargingInfo
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.NotificationSettingsNoWhiteList
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.RedirectToNotificationChannelSetting
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.RestoreHiddenCustomMediaAction
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.RestoreNearbyTile
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.UnlockControlCenterStyle
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.WaveCharge

object SystemUI : BasePackage() {
    override val packageName = "com.android.systemui"
    override val hooks = setOf(
        LockscreenChargingInfo,
        NotificationSettingsNoWhiteList,
        RedirectToNotificationChannelSetting,
        RestoreHiddenCustomMediaAction,
        RestoreNearbyTile,
        UnlockControlCenterStyle,
        WaveCharge,
    )
}