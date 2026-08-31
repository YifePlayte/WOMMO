package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import java.util.LinkedList

@Suppress("unused")
object DisableGestureRecorder : BaseHook() {
    override val key = "disable_gesture_recorder"
    override fun hook() {
        loadClass("com.android.systemui.statusbar.GestureRecorder").findMethod {
            name("save"); notAbstract()
        }.createHook {
            before {
                val mGestures =
                    it.thisObject.getFieldOrNull("mGestures") as LinkedList<*>?
                mGestures?.clear()
                it.result = null
            }
        }
    }
}