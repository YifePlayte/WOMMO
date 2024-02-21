package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullUntilSuperclassAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import de.robv.android.xposed.XposedHelpers.callMethod

@Suppress("unused")
object AllowMoveNonMIUIWidgetsToMinusScreen : BaseHook() {
    override val key = "allow_move_non_miui_widgets_to_minus_screen"
    override fun hook() {
        loadClass("com.miui.home.launcher.widget.MIUIWidgetHelper").methodFinder().filterByName("canDragToPa")
            .filterByParamCount(2).first().createHook {
                before { param ->
                    runCatching {
                        val dragInfo = invokeMethodBestMatch(param.args[1], "getDragInfo")!!
                        val spanX = getObjectOrNullUntilSuperclassAs<Int>(dragInfo, "spanX")!!
                        val spanY = getObjectOrNullUntilSuperclassAs<Int>(dragInfo, "spanY")!!
                        // val launcherCallBacks = invokeMethodBestMatch(param.args[0], "getLauncherCallbacks")
                        val launcherCallBacks = callMethod(param.args[0], "getLauncherCallbacks")
                        // val dragController = invokeMethodBestMatch(param.args[0], "getDragController")!!
                        val dragController = callMethod(param.args[0], "getDragController")
                        val isDraggingFromAssistant =
                            invokeMethodBestMatch(dragController, "isDraggingFromAssistant") as Boolean
                        val isDraggingToAssistant =
                            invokeMethodBestMatch(dragController, "isDraggingToAssistant") as Boolean
                        param.result =
                            launcherCallBacks != null && !isDraggingFromAssistant && !isDraggingToAssistant && spanX % 2 == 0 && (spanX != 2 || spanY == 2)
                    }
                }
            }
    }
}