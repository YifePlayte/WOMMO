package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.PowerManager
import android.util.ArrayMap
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNull
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.ClassUtils.loadFirstClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
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
        loadClassOrNull("com.android.systemui.statusbar.phone.KeyguardIndicationTextView")?.constructors?.createHooks {
            after { param ->
                (param.thisObject as TextView).isSingleLine = false
                val screenOnOffReceiver = object : BroadcastReceiver() {
                    val keyguardIndicationController = runCatching {
                        invokeStaticMethodBestMatch(
                            clazzDependency, "get", null, clazzKeyguardIndicationController
                        )!!
                    }.getOrElse {
                        val clazzMiuiStub = loadClass("miui.stub.MiuiStub")
                        val instanceMiuiStub = getStaticObjectOrNull(clazzMiuiStub, "INSTANCE")!!
                        val mSysUIProvider = getObjectOrNull(instanceMiuiStub, "mSysUIProvider")!!
                        val mKeyguardIndicationController =
                            getObjectOrNull(mSysUIProvider, "mKeyguardIndicationController")!!
                        invokeMethodBestMatch(mKeyguardIndicationController, "get")!!
                    }
                    val handler = Handler((param.thisObject as TextView).context.mainLooper)
                    val runnable = object : Runnable {
                        val clazzMiuiDependency = loadClass("com.miui.systemui.MiuiDependency")
                        val clazzMiuiChargeController =
                            loadClass("com.miui.charge.MiuiChargeController")
                        val sDependency =
                            getStaticObjectOrNull(clazzMiuiDependency, "sDependency")!!
                        val mProviders =
                            getObjectOrNull(sDependency, "mProviders") as ArrayMap<*, *>
                        val mMiuiChargeControllerProvider = mProviders[clazzMiuiChargeController]!!
                        val instanceMiuiChargeController = invokeMethodBestMatch(
                            mMiuiChargeControllerProvider, "createDependency"
                        )!!

                        override fun run() {
                            if (IS_HYPER_OS) {
                                doUpdateForHyperOS()
                            } else {
                                invokeMethodBestMatch(
                                    keyguardIndicationController, "updatePowerIndication"
                                )
                            }
                            handler.postDelayed(this, 1000)
                        }

                        fun doUpdateForHyperOS() {
                            val mBatteryStatus = getObjectOrNull(
                                instanceMiuiChargeController, "mBatteryStatus"
                            )!!
                            val level = getObjectOrNull(mBatteryStatus, "level")
                            val isPluggedIn = invokeMethodBestMatch(mBatteryStatus, "isPluggedIn")
                            val mContext = getObjectOrNull(instanceMiuiChargeController, "mContext")
                            val clazzChargeUtils = loadClass("com.miui.charge.ChargeUtils")
                            val chargingHintText = invokeStaticMethodBestMatch(
                                clazzChargeUtils,
                                "getChargingHintText",
                                null,
                                level,
                                isPluggedIn,
                                mContext
                            )
                            setObject(
                                keyguardIndicationController,
                                "mComputePowerIndication",
                                chargingHintText
                            )
                            invokeMethodBestMatch(
                                keyguardIndicationController,
                                "updateDeviceEntryIndication",
                                null,
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
        loadFirstClass(
            "com.miui.charge.ChargeUtils", "com.android.keyguard.charge.ChargeUtils"
        ).methodFinder().filterByName("getChargingHintText").filterByParamCount(3).first()
            .createHook {
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

            // val clazzMiuiChargeManager = loadClass("com.android.keyguard.charge.MiuiChargeManager")
            // val plugState = loadClass("com.android.systemui.Dependency").classHelper()
            //     .invokeStaticMethodBestMatch("get", null, clazzMiuiChargeManager)!!.objectHelper()
            //     .getObjectOrNull("mBatteryStatus")!!.objectHelper().getObjectOrNullAs<Int>("wireState")
            // when (plugState) {
            //     10 -> {
            //         current =
            //             FileReader("/sys/class/power_supply/wireless/rx_iout").use { fileReader ->
            //                 BufferedReader(fileReader).use { bufferedReader ->
            //                     bufferedReader.readLine().toDouble() / 1000000.0
            //                 }
            //             }
            //         voltage = FileReader("/sys/class/power_supply/wireless/input_voltage_vrect").use { fileReader ->
            //             BufferedReader(fileReader).use { bufferedReader ->
            //                 bufferedReader.readLine().toDouble() / 1000000.0
            //             }
            //         }
            //     }
            //
            //     else -> {
            //         current =
            //             FileReader("/sys/class/power_supply/usb/input_current_now").use { fileReader ->
            //                 BufferedReader(fileReader).use { bufferedReader ->
            //                     bufferedReader.readLine().toDouble() / 1000000.0
            //                 }
            //             }
            //         voltage = FileReader("/sys/class/power_supply/usb/voltage_now").use { fileReader ->
            //             BufferedReader(fileReader).use { bufferedReader ->
            //                 bufferedReader.readLine().toDouble() / 1000000.0
            //             }
            //         }
            //     }
            // }
            return String.format("%.2f A · %.2f V\n%.2f W", current, voltage, watt)
        }
        return EzXHelper.moduleRes.getString(R.string.lockscreen_charging_info_not_supported)
    }
}