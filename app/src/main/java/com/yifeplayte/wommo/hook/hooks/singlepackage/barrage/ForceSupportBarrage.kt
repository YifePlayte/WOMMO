package com.yifeplayte.wommo.hook.hooks.singlepackage.barrage

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import java.util.Vector

object ForceSupportBarrage : BaseHook() {
    override val key = "force_support_barrage"
    override fun hook() {
        loadClass("com.xiaomi.barrage.service.NotificationMonitorService").methodFinder()
            .filterByName("filterNotification").first().createHook {
                before { param ->
                    val statusBarNotification = param.args[0] as StatusBarNotification
                    val packageName = statusBarNotification.packageName
                    getObjectOrNullAs<ArrayList<String>>(
                        param.thisObject, "mBarragePackageList"
                    )!!.apply { if (!contains(packageName)) add(packageName) }
                    if (statusBarNotification.shouldBeFiltered()) {
                        param.result = true
                    }
                }
            }
    }

    object NotificationCache {
        private const val maxSize = 20
        private val cache = linkedSetOf<String>()
        fun check(string: String): Boolean {
            val result = cache.add(string)
            if (cache.size > maxSize) {
                cache.iterator().run {
                    next()
                    remove()
                }
            }
            return result
        }
    }

    private fun StatusBarNotification.shouldBeFiltered(): Boolean {
        val extras = notification.extras
        val key = "${extras.getCharSequence("android.title")}: ${extras.getCharSequence("android.text")}"
        val isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        return !isClearable || isGroupSummary || !NotificationCache.check(key)
    }
}