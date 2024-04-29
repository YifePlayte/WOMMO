package com.yifeplayte.wommo.hook.hooks.singlepackage.barrage

import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import java.lang.reflect.Method

@Suppress("unused")
object BarrageNotTouchable : BaseHook() {
    override val key = "barrage_not_touchable"
    override fun hook() {
        loadClass("com.xiaomi.barrage.utils.BarrageWindowUtils\$ComputeInternalInsetsHandler").methodFinder()
            .filterByName("invoke").filterNonAbstract().single().createHook {
                before { param ->
                    val method = param.args[1] as Method
                    if (!method.name.equals("onComputeInternalInsets")) return@before

                    val barrageWindowUtils = getObjectOrNull(param.thisObject, "this$0")!!

                    val mWindowParams =
                        getObjectOrNull(barrageWindowUtils, "mWindowParams") as LayoutParams
                    val mWindowManager =
                        getObjectOrNull(barrageWindowUtils, "mWindowManager") as WindowManager
                    val mView = getObjectOrNull(barrageWindowUtils, "mView") as View
                    val mWindowTouchable = getObjectOrNull(barrageWindowUtils, "mWindowTouchable")

                    if (mWindowTouchable == true || mWindowParams.flags and LayoutParams.FLAG_NOT_TOUCHABLE == 0) {
                        setObject(barrageWindowUtils, "mWindowTouchable", false)
                        mWindowParams.flags = mWindowParams.flags or LayoutParams.FLAG_NOT_TOUCHABLE
                        mWindowManager.updateViewLayout(mView, mWindowParams)
                    }

                    param.result = null
                }
            }
    }
}