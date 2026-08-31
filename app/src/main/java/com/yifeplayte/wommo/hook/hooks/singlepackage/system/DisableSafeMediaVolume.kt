package com.yifeplayte.wommo.hook.hooks.singlepackage.system

import com.yifeplayte.wommo.hook.hooks.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

@Suppress("unused")
object DisableSafeMediaVolume : BaseHook() {
    override val key = "disable_safe_media_volume"
    override fun hook() {
        loadClass("com.android.server.audio.SoundDoseHelperStubImpl").findAllMethods {
            name("updateSafeMediaVolumeIndex"); notAbstract(); paramCount(1)
        }.createHooks {
            returnConstant(0x7ffffffe)
        }
        loadClass("com.android.server.audio.SoundDoseHelper").findMethod {
            name("safeMediaVolumeIndex"); notAbstract()
        }.createHook {
            returnConstant(0x7ffffffe)
        }
    }
}