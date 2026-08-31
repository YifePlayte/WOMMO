package com.yifeplayte.wommo.hook.hooks.singlepackage.barrage

import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import java.lang.reflect.Method

@Suppress("unused")
object BarrageNotTouchable : BaseHook() {
    override val key = "barrage_not_touchable"
    override fun hook() {
        loadClass($$"com.xiaomi.barrage.utils.BarrageWindowUtils$ComputeInternalInsetsHandler").findMethod {
            name("invoke"); notAbstract()
        }.createHook {
            before { param ->
                val method = param.args[1] as Method
                if (!method.name.equals("onComputeInternalInsets")) return@before

                val barrageWindowUtils = param.thisObject.getFieldOrNull("this\$0")!!

                val mWindowParams =
                    barrageWindowUtils.getFieldOrNull("mWindowParams") as LayoutParams
                val mWindowManager =
                    barrageWindowUtils.getFieldOrNull("mWindowManager") as WindowManager
                val mView = barrageWindowUtils.getFieldOrNull("mView") as View
                val mWindowTouchable = barrageWindowUtils.getFieldOrNull("mWindowTouchable")

                if (mWindowTouchable == true || mWindowParams.flags and LayoutParams.FLAG_NOT_TOUCHABLE == 0) {
                    barrageWindowUtils.putField("mWindowTouchable", false)
                    mWindowParams.flags = mWindowParams.flags or LayoutParams.FLAG_NOT_TOUCHABLE
                    mWindowManager.updateViewLayout(mView, mWindowParams)
                }

                param.result = null
            }
        }
    }
}