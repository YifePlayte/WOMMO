package com.yifeplayte.wommo.hook.hooks.singlepackage.barrage

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getInt

@Suppress("unused")
object ModifyBarrageLength : BaseHook() {
    override val key = "modify_barrage_length"
    override val isEnabled get() = barrageLength != 36
    private val barrageLength by lazy { getInt("barrage_length", 36) }
    override fun hook() {
        val clazzString = loadClass("java.lang.String")
        clazzString.methodFinder().filterByName("subSequence").filterByParamCount(2).single()
            .createHook {
                before { param ->
                    if (Thread.currentThread().stackTrace.any { it.className == "com.xiaomi.barrage.utils.BarrageWindowUtils" }) {
                        param.args[1] = barrageLength
                    }
                }
                after {
                    if (it.throwable != null) {
                        it.throwable = null
                        it.result = it.thisObject
                    }
                }
            }
        clazzString.methodFinder().filterByName("length").filterByParamCount(0).single()
            .createHook {
                after { param ->
                    val stacktrace = Thread.currentThread().stackTrace
                    if (stacktrace.any {
                            it.className in setOf(
                                "java.lang.String", "android.text.SpannableStringBuilder"
                            )
                        }) return@after
                    if (stacktrace.any {
                            it.className == "com.xiaomi.barrage.utils.BarrageWindowUtils" && it.methodName in setOf(
                                "addBarrageNotification", "sendBarrage"
                            )
                        }) {
                        val realResult = (param.result as Int)
                        param.result = if (barrageLength < 36) {
                            if (realResult > barrageLength) {
                                maxOf(37, realResult)
                            } else realResult
                        } else {
                            if (realResult <= barrageLength) {
                                minOf(35, realResult)
                            } else realResult
                        }
                    }
                }
            }
    }
}