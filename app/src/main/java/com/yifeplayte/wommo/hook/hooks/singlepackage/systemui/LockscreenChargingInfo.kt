package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
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
import com.yifeplayte.wommo.utils.Build.HYPER_OS_VERSION
import com.yifeplayte.wommo.utils.Build.IS_HYPER_OS
import java.io.BufferedReader
import java.io.FileReader
import java.lang.ref.WeakReference

@Suppress("unused")
object LockscreenChargingInfo : BaseHook() {
    override val key = "lockscreen_charging_info"

    override fun hook() {
        if (HYPER_OS_VERSION >= 4) hookHyperOS4() else hookLegacy()
    }

    private const val INDICATION_TYPE_BATTERY = 3

    /** IMiCharge 的 transaction code，取自 vendor.xiaomi.hardware.micharge-V2-ndk.so 的 BpMiCharge */
    private const val MICHARGE_SERVICE = "vendor.xiaomi.hardware.micharge.IMiCharge/default"
    private const val MICHARGE_DESCRIPTOR = "vendor.xiaomi.hardware.micharge.IMiCharge"
    private const val MICHARGE_CODE_USB_CURRENT = 0x1c
    private const val MICHARGE_CODE_USB_VOLTAGE = 0x1d

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var rotateTextViewController: WeakReference<Any>? = null
    private var batteryMessage: CharSequence? = null
    private var screenReceiver: BroadcastReceiver? = null

    /**
     * HyperOS 4 移除了 getChargingHintText / updatePowerIndication / mComputePowerIndication
     * 这条充电文案链路：充电提示由 KeyguardIndicationController.updateDeviceEntryIndication
     * 就地拼出，再交给 KeyguardIndicationRotateTextViewController.updateIndication(3, ...) 上屏
     */
    private fun hookHyperOS4() {
        loadClass("com.android.systemui.statusbar.phone.KeyguardIndicationTextView")
            .constructors.toList().createHooks {
                after { param ->
                    val textView = param.thisObject as TextView
                    textView.isSingleLine = false
                    startRefreshLoop(textView.context)
                }
            }
        loadClass("com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController")
            .findMethod {
                name("updateIndication"); paramCount(3)
            }.createHook {
                before { param ->
                    if (param.args[0] as? Int != INDICATION_TYPE_BATTERY) return@before
                    val indication = param.args[1] ?: return@before
                    val message = indication.getFieldOrNull("mMessage") as? CharSequence
                        ?: return@before
                    val context = (param.thisObject.getFieldOrNull("mView") as? TextView)
                        ?.context ?: return@before
                    rotateTextViewController = WeakReference(param.thisObject)
                    batteryMessage = message
                    val chargingInfo = getChargingInfoForHyperOS4(context) ?: return@before
                    indication.putField("mMessage", "$message\n$chargingInfo")
                }
            }
    }

    /**
     * 功率每秒都在变，而框架只在电池事件到达时才重刷文案，
     * 因此按秒重写文本内容。只改 TextView，不动轮播控制器的状态，
     * 否则每次都走 updateDeviceEntryIndication 会把别的锁屏提示挤出轮播
     */
    private val refreshRunnable = object : Runnable {
        override fun run() {
            mainHandler.postDelayed(this, 1000)
            val controller = rotateTextViewController?.get() ?: return
            if ((controller.getFieldOrNull("mCurrIndicationType") as? Int)
                != INDICATION_TYPE_BATTERY
            ) return
            val view = controller.getFieldOrNull("mView") as? TextView ?: return
            if (!view.isShown) return
            val message = batteryMessage ?: return
            val chargingInfo = getChargingInfoForHyperOS4(view.context)
            val text = if (chargingInfo == null) "$message" else "$message\n$chargingInfo"
            if (view.text.toString() == text) return
            view.text = text
        }
    }

    private fun startRefreshLoop(context: Context) {
        if (screenReceiver != null) return
        val appContext = context.applicationContext ?: context
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> mainHandler.post(refreshRunnable)
                    Intent.ACTION_SCREEN_OFF -> mainHandler.removeCallbacks(refreshRunnable)
                }
            }
        }
        appContext.registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        screenReceiver = receiver
        if ((appContext.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive) {
            mainHandler.post(refreshRunnable)
        }
    }

    /**
     * 电池侧的 current_now / voltage_now 在 platform_app 域可读，与旧路径一致；
     * usb / wireless 节点属于 vendor_sysfs_usb_supply，实测被 SELinux 拒绝（EACCES）。
     * 输入侧的功率改从小米充电 HAL 取：IMiCharge 的 getUsbVoltage / getUsbCurrent
     * 返回微伏、微安的字符串，platform_app 域对该 binder 有 call 权限。
     * 无线充电时 usb 节点 ONLINE=0 但 VOLTAGE_NOW / CURRENT_NOW 仍是接收端整流后的
     * 直流输入（10.9V 量级），这两个 HAL 方法在新旧两种供电下都返回该值
     */
    private fun getChargingInfoForHyperOS4(context: Context): String? {
        val batteryStatus = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return null
        if (batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) == 0) return null
        val usbVoltage = queryMiCharge(MICHARGE_CODE_USB_VOLTAGE)
        val usbCurrent = queryMiCharge(MICHARGE_CODE_USB_CURRENT)
        if (usbVoltage > 0 && usbCurrent > 0) {
            val current = usbCurrent / 1000000.0
            val voltage = usbVoltage / 1000000.0
            return String.format("%.2f A · %.2f V\n%.2f W", current, voltage, current * voltage)
        }
        return getChargingInfo()
    }

    /**
     * 直接发 transaction：接口有几十个方法且多数带字符串参数，
     * 只为两个无参查询引入整套 AIDL 代理解析不值得
     */
    private fun queryMiCharge(code: Int): Int = runCatching {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, MICHARGE_SERVICE) as? android.os.IBinder
            ?: return@runCatching -1
        val data = android.os.Parcel.obtain()
        val reply = android.os.Parcel.obtain()
        try {
            data.writeInterfaceToken(MICHARGE_DESCRIPTOR)
            if (!binder.transact(code, data, reply, 0)) return@runCatching -1
            reply.readException()
            reply.readString()?.toIntOrNull() ?: -1
        } finally {
            reply.recycle()
            data.recycle()
        }
    }.getOrElse { -1 }

    private fun hookLegacy() {
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
