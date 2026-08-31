package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object WaveCharge : BaseHook() {
    override val key = "wave_charge"
    override fun hook() {
        loadClass("com.android.keyguard.charge.ChargeUtils").findMethod {
            name("supportWaveChargeAnimation")
        }.createHook {
            after { param ->
                val clazzTrue = setOf(
                    "com.android.keyguard.charge.ChargeUtils",
                    "com.android.keyguard.charge.container.MiuiChargeContainerView"
                )
                param.result =
                    Thread.currentThread().stackTrace.any { it.className in clazzTrue }
            }
        }
        loadClass("com.android.keyguard.charge.wave.WaveView").findMethod {
            name("updateWaveHeight")
        }.createHook {
            after {
                it.thisObject.putField("mWaveXOffset", 0)
            }
        }
    }
}