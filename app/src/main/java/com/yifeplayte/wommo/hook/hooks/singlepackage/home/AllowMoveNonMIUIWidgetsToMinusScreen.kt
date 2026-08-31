package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.Log

@Suppress("unused")
object AllowMoveNonMIUIWidgetsToMinusScreen : BaseHook() {
    override val key = "allow_move_non_miui_widgets_to_minus_screen"
    override fun hook() {
        loadClass("com.miui.home.launcher.widget.MIUIWidgetHelper").findMethod {
            name("canDragToPa"); paramCount(2)
        }.createHook {
            before { param ->
                runCatching {
                    val dragInfo = param.args[1]!!.callMethod("getDragInfo")!!
                    val spanX = dragInfo.getFieldOrNullAs<Int>("spanX")!!
                    val spanY = dragInfo.getFieldOrNullAs<Int>("spanY")!!
                    val clazzBaseLauncher = loadClass("com.miui.home.launcher.BaseLauncher")
                    val launcherCallBacks = clazzBaseLauncher.getDeclaredMethod("getLauncherCallbacks").invoke(param.args[0])
                    val dragController = clazzBaseLauncher.getDeclaredMethod("getDragController").invoke(param.args[0])
                    val isDraggingFromAssistant =
                        dragController.callMethod("isDraggingFromAssistant") as Boolean
                    val isDraggingToAssistant =
                        dragController.callMethod("isDraggingToAssistant") as Boolean
                    param.result =
                        launcherCallBacks != null && !isDraggingFromAssistant && !isDraggingToAssistant && spanX % 2 == 0 && (spanX != 2 || spanY == 2)
                }.onFailure {
                    Log.e("AllowMoveNonMIUIWidgetsToMinusScreen failed", it)
                }
            }
        }
    }
}