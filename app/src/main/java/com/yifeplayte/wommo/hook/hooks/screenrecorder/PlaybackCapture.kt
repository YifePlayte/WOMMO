package com.yifeplayte.wommo.hook.hooks.screenrecorder

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.MemberExtensions.paramCount
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

object PlaybackCapture : BaseHook() {
    override fun init() {
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