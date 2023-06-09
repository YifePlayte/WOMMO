package com.yifeplayte.wommo.hook.hooks.singlepackage.barrage

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getInt

object ModifyBarrageLength : BaseHook() {
    override val key = "modify_barrage_length"
    override val isEnabled: Boolean
        get() = barrageLength != 36
    private val barrageLength by lazy {
        getInt("barrage_length", 36)
    }

    override fun hook() {
        val clazzString = loadClass("java.lang.String")
        clazzString.methodFinder().filterByName("subSequence").filterByParamCount(2).first().createHook {
            before { param ->
                if (Throwable().stackTrace.any { it.className == "com.xiaomi.barrage.utils.BarrageWindowUtils" }) {
                    param.args[1] = barrageLength
                }
            }
        }
        clazzString.methodFinder().filterByName("length").filterByParamCount(0).first().createHook {
            after { param ->
                val stacktrace = Throwable().stackTrace
                if (stacktrace.any { it.className == "java.lang.String" }) return@after
                if (stacktrace.firstOrNull { it.className == "com.xiaomi.barrage.utils.BarrageWindowUtils" }?.methodName in setOf(
                        "addBarrageNotification", "sendBarrage"
                    )
                ) {
                    val realResult = (param.result as Int)
                    param.result = if (barrageLength < 36) {
                        if (realResult > barrageLength) {
                            maxOf(37, realResult)
                        } else realResult
                    } else {
                        if (realResult < barrageLength) {
                            minOf(35, realResult)
                        } else realResult
                    }
                }
            }
        }
    }
}