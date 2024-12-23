package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullUntilSuperclass
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import java.util.LinkedList

@Suppress("unused")
object DisableGestureRecorder : BaseHook() {
    override val key = "disable_gesture_recorder"
    override fun hook() {
        loadClass("com.android.systemui.statusbar.GestureRecorder").methodFinder()
            .filterByName("save").filterNonAbstract().single().createHook {
                before {
                    val mGestures =
                        getObjectOrNullUntilSuperclass(it.thisObject, "mGestures") as LinkedList<*>?
                    mGestures?.clear()
                    it.result = null
                }
            }
    }
}