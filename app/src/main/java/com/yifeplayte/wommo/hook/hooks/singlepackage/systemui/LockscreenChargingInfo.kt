package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.PowerManager
import android.util.ArrayMap
import android.widget.TextView
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.getStaticFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.loadClassFirst
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_HYPER_OS
import java.io.BufferedReader
import java.io.FileReader

@Suppress("unused")
object LockscreenChargingInfo : BaseHook() {
    override val key = "lockscreen_charging_info"
    override fun hook() {
        val clazzDependency = loadClass("com.android.systemui.Dependency")
        val clazzKeyguardIndicationController =
            loadClass("com.android.systemui.statusbar.KeyguardIndicationController")
        loadClassOrNull("com.android.systemui.statusbar.phone.KeyguardIndicationTextView")?.constructors?.toList()?.createHooks {
            after { param ->
                (param.thisObject as TextView).isSingleLine = false
                val screenOnOffReceiver = object : BroadcastReceiver() {
                    val keyguardIndicationController = runCatching {
                        clazzDependency.callStaticMethod(
                            "get", clazzKeyguardIndicationController
                        )!!
                    }.getOrElse {
                        val clazzMiuiStub = loadClass("miui.stub.MiuiStub")
                        val instanceMiuiStub = clazzMiuiStub.getStaticFieldOrNull("INSTANCE")!!
                        val mSysUIProvider = instanceMiuiStub.getFieldOrNull("mSysUIProvider")!!
                        val mKeyguardIndicationController =
                            mSysUIProvider.getFieldOrNull("mKeyguardIndicationController")!!
                        mKeyguardIndicationController.callMethod("get")!!
                    }
                    val handler = Handler((param.thisObject as TextView).context.mainLooper)
                    val runnable = object : Runnable {
                        val clazzMiuiDependency = loadClass("com.miui.systemui.MiuiDependency")
                        val clazzMiuiChargeController =
                            loadClass("com.miui.charge.MiuiChargeController")
                        val sDependency =
                            clazzMiuiDependency.getStaticFieldOrNull("sDependency")!!
                        val mProviders =
                            sDependency.getFieldOrNull("mProviders") as ArrayMap<*, *>
                        val mMiuiChargeControllerProvider = mProviders[clazzMiuiChargeController]!!
                        val instanceMiuiChargeController = mMiuiChargeControllerProvider.callMethod(
                            "createDependency"
                        )!!

                        override fun run() {
                            if (IS_HYPER_OS) {
                                doUpdateForHyperOS()
                            } else {
                                keyguardIndicationController.callMethod("updatePowerIndication")
                            }
                            handler.postDelayed(this, 1000)
                        }

                        fun doUpdateForHyperOS() {
                            val mBatteryStatus = instanceMiuiChargeController.getFieldOrNull(
                                "mBatteryStatus"
                            )!!
                            val level = mBatteryStatus.getFieldOrNull("level")
                            val isPluggedIn = mBatteryStatus.callMethod("isPluggedIn")
                            val mContext = instanceMiuiChargeController.getFieldOrNull("mContext")
                            val clazzChargeUtils = loadClass("com.miui.charge.ChargeUtils")
                            val chargingHintText = clazzChargeUtils.callStaticMethod(
                                "getChargingHintText",
                                level,
                                isPluggedIn,
                                mContext
                            )
                            keyguardIndicationController.putField(
                                "mComputePowerIndication",
                                chargingHintText
                            )
                            keyguardIndicationController.callMethod(
                                "updateDeviceEntryIndication",
                                false
                            )
                        }
                    }

                    init {
                        if (((param.thisObject as TextView).context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive) {
                            handler.post(runnable)
                        }
                    }

                    override fun onReceive(context: Context, intent: Intent) {
                        when (intent.action) {
                            Intent.ACTION_SCREEN_ON -> {
                                handler.post(runnable)
                            }

                            Intent.ACTION_SCREEN_OFF -> {
                                handler.removeCallbacks(runnable)
                            }
                        }
                    }
                }

                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                }
                (param.thisObject as TextView).context.registerReceiver(
                    screenOnOffReceiver, filter
                )
            }
        }
        loadClassFirst(
            "com.miui.charge.ChargeUtils", "com.android.keyguard.charge.ChargeUtils"
        ).findMethod { name("getChargingHintText"); paramCount(3) }.createHook {
            after { param ->
                param.result = param.result?.let { "$it\n${getChargingInfo()}" }
            }
        }
    }

    private fun getChargingInfo(): String {
        kotlin.runCatching {
            var current = 0.0
            var voltage = 0.0
            val watt: Double by lazy {
                current * voltage
            }

            current = FileReader("/sys/class/power_supply/battery/current_now").use { fileReader ->
                BufferedReader(fileReader).use { bufferedReader ->
                    -1.0 * bufferedReader.readLine().toDouble() / 1000000.0
                }
            }
            voltage = FileReader("/sys/class/power_supply/battery/voltage_now").use { fileReader ->
                BufferedReader(fileReader).use { bufferedReader ->
                    bufferedReader.readLine().toDouble() / 1000000.0
                }
            }

            return String.format("%.2f A · %.2f V\n%.2f W", current, voltage, watt)
        }
        return EzXposed.moduleRes.getString(R.string.lockscreen_charging_info_not_supported)
    }
}
