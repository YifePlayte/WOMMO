package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.screenrecorder.ForceEnableNativePlaybackCapture
import com.yifeplayte.wommo.hook.hooks.singlepackage.screenrecorder.ModifyScreenRecorderConfig

object ScreenRecorder : BasePackage() {
    override val packageName = "com.miui.screenrecorder"
    override val hooks = setOf(
        ForceEnableNativePlaybackCapture,
        ModifyScreenRecorderConfig,
    )
}