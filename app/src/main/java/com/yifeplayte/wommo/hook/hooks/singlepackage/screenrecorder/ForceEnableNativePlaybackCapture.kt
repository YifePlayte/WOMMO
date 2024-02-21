package com.yifeplayte.wommo.hook.hooks.singlepackage.screenrecorder

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.paramCount
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object ForceEnableNativePlaybackCapture : BaseHook() {
    override val key = "force_enable_native_playback_capture"
    override fun hook() {
        loadClass("android.os.SystemProperties").methodFinder().first {
            name == "getBoolean"
                    && paramCount == 2
                    && parameterTypes[0] == String::class.java
                    && parameterTypes[1] == Boolean::class.java
        }.createHook {
            before { param ->
                if (param.args[0] == "ro.vendor.audio.playbackcapture.screen") {
                    param.result = true
                }
            }
        }
    }
}