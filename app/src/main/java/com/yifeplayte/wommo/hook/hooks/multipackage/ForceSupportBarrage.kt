package com.yifeplayte.wommo.hook.hooks.multipackage

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.ApplicationInfo
import android.service.notification.StatusBarNotification
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putStaticField
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getInstance
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import io.github.ranlee1.jpinyin.PinyinFormat.WITHOUT_TONE
import io.github.ranlee1.jpinyin.PinyinHelper.convertToPinyinString
import java.io.Serial

@Suppress("unused")
object ForceSupportBarrage : BaseMultiHook() {
    override val key = "force_support_barrage"
    override val hooks = mapOf(
        "com.xiaomi.barrage" to { hookForBarrage() },
        "com.miui.securitycenter" to { hookForSecurityCenter() },
    )

    @SuppressLint("QueryPermissionsNeeded")
    private fun hookForSecurityCenter() {
        val clazzNotificationFilterHelper = loadClass("miui.util.NotificationFilterHelper")
        val methodAreNotificationsEnabled = clazzNotificationFilterHelper.getDeclaredMethod(
            "areNotificationsEnabled", Context::class.java, String::class.java
        ).apply { isAccessible = true }
        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("game_box_barrage_v3_support_apps.json")
            }
        }.single().getInstance().findMethod {
            returnType(java.util.List::class.java); paramCount(1)
        }.createHook {
            after { param ->
                val barragePackageList = EzXposed.appContext.packageManager.getInstalledApplications(0)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 1 }.filter {
                        methodAreNotificationsEnabled.invoke(
                            clazzNotificationFilterHelper, EzXposed.appContext, it.packageName
                        ) == true
                    }.associateWith {
                        val label = it.loadLabel(EzXposed.appContext.packageManager).toString()
                        convertToPinyinString(label, "", WITHOUT_TONE).lowercase()
                    }.entries.sortedBy { it.value }.map { it.key.packageName }
                @Suppress("UNCHECKED_CAST") val supportedList = param.result as MutableList<String>
                for (s in barragePackageList) {
                    if (!supportedList.contains(s)) supportedList.add(s)
                }
            }
        }
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("isApplicationFloatNotificationEnable fail ")
            }
        }.single().getMethodInstance().createHook {
            returnConstant(true)
        }
    }

    private fun hookForBarrage() {
        val clazzNotificationMonitorService =
            loadClass("com.xiaomi.barrage.service.NotificationMonitorService")
        clazzNotificationMonitorService.putStaticField(
            "mBarragePackageList",
            object : ArrayList<String?>() {
                @Serial
                private val serialVersionUID: Long = 1643198520517506969L
                override fun contains(element: String?): Boolean {
                    return true
                }
            })
        clazzNotificationMonitorService.findMethod { name("filterNotification") }.createHook {
            before { param ->
                val statusBarNotification = param.args[0] as StatusBarNotification
                if (statusBarNotification.shouldBeFiltered()) param.result = true
            }
        }
    }

    object NotificationCache {
        private const val MAX_SIZE = 100
        private val cache = LinkedHashSet<String>()
        fun check(string: String): Boolean {
            val result = cache.add(string)
            if (cache.size > MAX_SIZE) cache.remove(cache.first())
            return result
        }
    }

    private fun StatusBarNotification.shouldBeFiltered(): Boolean {
        val extras = notification.extras
        val key =
            "${extras.getCharSequence("android.title")}: ${extras.getCharSequence("android.text")}"
        val isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        return !isClearable || isGroupSummary || !NotificationCache.check(key)
    }
}
