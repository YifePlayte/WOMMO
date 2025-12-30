package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.LogExtensions.logdxIfThrow
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullUntilSuperclassAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object AllowMoveNonMIUIWidgetsToMinusScreen : BaseHook() {
    override val key = "allow_move_non_miui_widgets_to_minus_screen"
    override fun hook() {
        loadClass("com.miui.home.launcher.widget.MIUIWidgetHelper").methodFinder()
            .filterByName("canDragToPa").filterByParamCount(2).single().createHook {
                before { param ->
                    runCatching {
                        val dragInfo = invokeMethodBestMatch(param.args[1], "getDragInfo")!!
                        val spanX = getObjectOrNullUntilSuperclassAs<Int>(dragInfo, "spanX")!!
                        val spanY = getObjectOrNullUntilSuperclassAs<Int>(dragInfo, "spanY")!!
                        val clazzBaseLauncher = loadClass("com.miui.home.launcher.BaseLauncher")
                        val launcherCallBacks = clazzBaseLauncher.getDeclaredMethod("getLauncherCallbacks").invoke(param.args[0])
                        val dragController = clazzBaseLauncher.getDeclaredMethod("getDragController").invoke(param.args[0])
                        val isDraggingFromAssistant =
                            invokeMethodBestMatch(dragController, "isDraggingFromAssistant") as Boolean
                        val isDraggingToAssistant =
                            invokeMethodBestMatch(dragController, "isDraggingToAssistant") as Boolean
                        param.result =
                            launcherCallBacks != null && !isDraggingFromAssistant && !isDraggingToAssistant && spanX % 2 == 0 && (spanX != 2 || spanY == 2)
                    }.logdxIfThrow()
                }
            }
    }
}