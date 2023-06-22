package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

object WaveCharge : BaseHook() {
    override val key = "wave_charge"
    override fun hook() {
        loadClass("com.android.keyguard.charge.ChargeUtils").methodFinder().filterByName("supportWaveChargeAnimation")
            .first().createHook {
                after { param ->
                    val clazzTrue = setOf(
                        "com.android.keyguard.charge.ChargeUtils",
                        "com.android.keyguard.charge.container.MiuiChargeContainerView"
                    )
                    param.result = Throwable().stackTrace.any { it.className in clazzTrue }
                }
            }
        loadClass("com.android.keyguard.charge.wave.WaveView").methodFinder().filterByName("updateWaveHeight").first()
            .createHook {
                after {
                    it.thisObject.objectHelper().setObject("mWaveXOffset", 0)
                }
            }
    }
}