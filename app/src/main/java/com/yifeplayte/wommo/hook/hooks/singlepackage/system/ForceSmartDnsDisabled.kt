package com.yifeplayte.wommo.hook.hooks.singlepackage.system

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import android.content.ContentResolver
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.Log

/**
 * Returns a fixed value to the SmartDns cloud-config reader while leaving the
 * stored setting untouched. With SmartDns disabled, `resolv_getaddrinfo` no
 * longer gates AAAA lookups on the per-network IPv6 probe.
 */
@Suppress("unused")
object ForceSmartDnsDisabled : BaseHook() {
    override val key = "force_smart_dns_disabled"

    private const val KEY_V2 = "cloud_smartdns_enabled_v2"
    private const val KEY_V1 = "cloud_smartdns_enabled"

    // enable,ipv6_timeout,max_queries_global,ipv4_first,retransmission,retry_count,
    // no_retry_after_cancel,dns_thread_restricted_enable,warning_high,warning_low,
    // queries_restricted
    // No segment may contain "on": the status change code does a substring match
    // over the whole string to derive smartdns_enable, so a "on" anywhere keeps
    // the switch on. "off" and "0" are safe.
    private const val OVERRIDE = "off,1000,2500,off,2500,2,1,1,560,420,128"

    override fun hook() {
        loadClass("android.provider.Settings\$System").findMethod {
            name("getStringForUser")
            paramCount(3)
            params(ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType!!)
        }.createHook {
            before { param ->
                val key = param.args[1] as? String ?: return@before
                if (key != KEY_V2 && key != KEY_V1) return@before
                Log.i("ForceSmartDnsDisabled: $key -> $OVERRIDE")
                param.result = OVERRIDE
            }
        }
    }
}
