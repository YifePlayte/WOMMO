package com.yifeplayte.wommo.hook.hooks.multipackage

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.ApplicationInfo
import android.service.notification.StatusBarNotification
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
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
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("getInstance().assets.open(_SUPPORT_APPS_FILE_NAME)")
            }
        }.single().getMethodInstance(safeClassLoader).createHook {
            after { param ->
                val barragePackageList = appContext.packageManager.getInstalledApplications(0)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 1 }.filter {
                        methodAreNotificationsEnabled.invoke(
                            clazzNotificationFilterHelper, appContext, it.packageName
                        ) == true
                    }.associateWith {
                        val label = it.loadLabel(appContext.packageManager).toString()
                        convertToPinyinString(label, "", WITHOUT_TONE).lowercase()
                    }.entries.sortedBy { it.value }.map { it.key.packageName }
                @Suppress("UNCHECKED_CAST") val supportedList = param.result as MutableList<String>
                for (s in barragePackageList) {
                    if (!supportedList.contains(s)) supportedList.add(s)
                }
            }
        }
    }

    private fun hookForBarrage() {
        val clazzNotificationMonitorService =
            loadClass("com.xiaomi.barrage.service.NotificationMonitorService")
        setStaticObject(
            clazzNotificationMonitorService,
            "mBarragePackageList",
            object : ArrayList<String?>() {
                @Serial
                private val serialVersionUID: Long = 1643198520517506969L
                override fun contains(element: String?): Boolean {
                    return true
                }
            })
        clazzNotificationMonitorService.methodFinder().filterByName("filterNotification").single()
            .createHook {
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