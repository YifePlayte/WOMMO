package com.yifeplayte.wommo.hook.hooks.singlepackage.system

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.Log
import com.yifeplayte.wommo.hook.utils.XSharedPreferences
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.io.File

/**
 * Publishes the native hook switch values to the HYOS launcher family.
 *
 * MiuiHome, weather and gallery are forked by hyos_spawner and own no ART
 * runtime, so neither the module's Java hooks nor the framework's remote
 * preference store are reachable from inside them.  Their native hooks can only
 * ask for the switch snapshot before installing a patch, and this endpoint
 * answers that question from system_server - the process LSPosed already scopes.
 *
 * It owns one private transaction on the clipboard service: only a genuine
 * IClipboard call frame, from a process that maps libwommo_native.so
 * (uid + pid + cmdline verified), is answered; unreadable preferences answer
 * "unavailable" so the native side fails closed.
 *
 * Infrastructure rather than a user feature, hence [isEnabled] is always true.
 */
@Suppress("unused")
object NativeConfigEndpoint : BaseHook() {
    override val key = "native_config_endpoint"
    override val isEnabled get() = true

    /** Keep in sync with app/src/main/cpp/hooks/native_config.cpp */
    private const val STUB_CLASS = "android.content.IClipboard\$Stub"
    private const val DESCRIPTOR = "android.content.IClipboard"
    private const val TRANSACTION_CODE = 0x00574F4D // "WOM"
    private const val ACK = 0x574F4D31 // "WOM1"

    /**
     * Preference key of each feature bit, in bit order.
     *
     * A feature that also exists on the legacy launcher reuses the Java hook's
     * key, so the one switch keeps covering both launcher generations.
     */
    private val featureKeys = arrayOf(
        "enable_perfect_icons",
        "hide_landscape_nav_bar",
        "allow_move_non_miui_widgets_to_minus_screen",
        "allow_search_on_4x7",
        "restore_back_home_ratio",
        "joystick_touch_guard",
        "disable_icon_highlight",
    )

    /** Processes that map libwommo_native.so; extend together with the native targets. */
    private val nativeHookProcesses =
        arrayOf("com.miui.home", "com.miui.weather2", "com.miui.gallery")

    private var publishedFlags = -1
    private var revision = 0L
    private var rejectedCallerLogged = false
    private var failureLogged = false

    override fun hook() {
        // The first remote preference lookup performs one Binder round trip to
        // the framework; warm it here so the transaction path stays free of
        // nested IPC.
        readFlags()
        // The clipboard service is the carrier: HyperOS hides `window` from
        // third-party processes, and clipboard traffic is far colder than a
        // window transaction, so a module-owned code on its Stub is cheap.
        loadClass(STUB_CLASS).findMethod {
            name("onTransact")
            paramCount(4)
        }.createHook {
            before { param ->
                if (param.args[0] as Int != TRANSACTION_CODE) return@before
                val data = param.args[1] as Parcel
                val reply = param.args[2] as? Parcel ?: return@before
                if (!answer(data, reply, param.args[3] as Int)) return@before
                // This private code is answered here, so the service must not handle it.
                param.result = true
            }
        }
    }

    /** @return true when this endpoint answered; the private code never reaches the service. */
    private fun answer(data: Parcel, reply: Parcel, binderFlags: Int): Boolean {
        // This hook sits on system_server's transaction path: every failure must
        // stay inside this method so the surrounding transaction keeps its
        // original meaning.
        return runCatching { answerInternal(data, reply, binderFlags) }
            .getOrElse { failure ->
                if (!failureLogged) {
                    failureLogged = true
                    Log.e("Native config endpoint failed", failure)
                }
                false
            }
    }

    @Synchronized
    private fun answerInternal(data: Parcel, reply: Parcel, binderFlags: Int): Boolean {
        val uid = Binder.getCallingUid()
        val pid = Binder.getCallingPid()
        if (binderFlags and IBinder.FLAG_ONEWAY != 0) return false
        val position = data.dataPosition()
        val validFrame = runCatching { data.enforceInterface(DESCRIPTOR) }
            .isSuccess && data.dataAvail() == 0
        data.setDataPosition(position)
        // A frame that is not ours stays untouched: the service then rejects the
        // unknown code exactly like it does without this module.
        if (!validFrame) return false
        if (!isNativeHookProcess(uid, pid)) {
            if (!rejectedCallerLogged) {
                rejectedCallerLogged = true
                Log.w("Native config query rejected: uid=$uid pid=$pid")
            }
            return false
        }
        writeReply(reply, readFlags())
        return true
    }

    /** @return null when the remote preference store is unreadable */
    private fun readFlags(): Int? = runCatching {
        var flags = 0
        featureKeys.forEachIndexed { index, key ->
            if (XSharedPreferences.getBoolean(key, false)) flags = flags or (1 shl index)
        }
        flags
    }.getOrNull()

    private fun writeReply(reply: Parcel, flags: Int?) {
        val available = flags != null
        val value = flags ?: 0
        if (available && value != publishedFlags) {
            publishedFlags = value
            revision++
        }
        reply.setDataSize(0)
        reply.setDataPosition(0)
        reply.writeInt(ACK)
        reply.writeInt(if (available) 1 else 0)
        reply.writeInt(value)
        reply.writeLong(revision)
    }

    /**
     * uid + pid + cmdline triple check.
     *
     * A synchronous binder transaction carries the real calling pid, so the
     * process name can be confirmed and its uid cross-checked against the uid
     * Binder reports.
     */
    private fun isNativeHookProcess(uid: Int, pid: Int): Boolean {
        if (uid < 10000 || pid <= 0) return false
        val commandLine = runCatching { File("/proc/$pid/cmdline").readBytes() }
            .getOrNull() ?: return false
        val processName = String(commandLine, Charsets.UTF_8).substringBefore('\u0000')
        if (processName !in nativeHookProcesses) return false
        return processUid(pid) == uid
    }

    private fun processUid(pid: Int): Int? = runCatching {
        File("/proc/$pid/status").readLines()
            .firstOrNull { it.startsWith("Uid:") }
            ?.substringAfter("Uid:")
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.firstOrNull()
            ?.toInt()
    }.getOrNull()
}
