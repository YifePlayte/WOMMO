package com.yifeplayte.wommo.hook.hooks.singlepackage.screenrecorder

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object ForceEnableNativePlaybackCapture : BaseHook() {
    override val key = "force_enable_native_playback_capture"
    override fun hook() {
        // 原实现用 Boolean::class.java 匹配 boolean 基本类型参数，永远匹配不到；
        // 这里按 SystemProperties.getBoolean(String, boolean) 的真实签名匹配
        loadClass("android.os.SystemProperties").findMethod {
            name("getBoolean")
            paramCount(2)
            params(String::class.java, Boolean::class.javaPrimitiveType!!)
        }.createHook {
            before { param ->
                if (param.args[0] == "ro.vendor.audio.playbackcapture.screen") {
                    param.result = true
                }
            }
        }
    }
}