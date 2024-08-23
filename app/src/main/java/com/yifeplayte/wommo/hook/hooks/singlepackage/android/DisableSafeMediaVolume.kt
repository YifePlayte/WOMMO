package com.yifeplayte.wommo.hook.hooks.singlepackage.android

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object DisableSafeMediaVolume : BaseHook() {
    override val key = "disable_safe_media_volume"
    override fun hook() {
        loadClass("com.android.server.audio.SoundDoseHelperStubImpl").methodFinder()
            .filterByName("updateSafeMediaVolumeIndex").filterNonAbstract().filterByParamCount(1)
            .toList().createHooks {
                returnConstant(0x7ffffffe)
            }
        loadClass("com.android.server.audio.SoundDoseHelper").methodFinder()
            .filterByName("safeMediaVolumeIndex").filterNonAbstract().single().createHook {
                returnConstant(0x7ffffffe)
            }
    }
}