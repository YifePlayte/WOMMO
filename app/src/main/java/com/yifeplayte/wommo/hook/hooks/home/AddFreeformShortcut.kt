package com.yifeplayte.wommo.hook.hooks.home

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.view.View.OnClickListener
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.hook.hooks.BaseHook
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.callStaticMethod
import de.robv.android.xposed.XposedHelpers.getStaticObjectField
import de.robv.android.xposed.XposedHelpers.setStaticObjectField

object AddFreeformShortcut : BaseHook() {
    @SuppressLint("DiscouragedApi")
    override fun init() {
        val clazzSystemShortcutMenuItem = loadClass("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem")

        Activity::class.java.methodFinder().filterByName("onCreate").toList().createHooks {
            after {
                EzXHelper.initAppContext(it.thisObject as Activity)
            }
        }
        loadClass("com.miui.home.launcher.util.ViewDarkModeHelper").methodFinder()
            .filterByName("onConfigurationChanged").toList().createHooks {
                after {
                    invokeStaticMethodBestMatch(clazzSystemShortcutMenuItem, "createAllSystemShortcutMenuItems")
                }
            }
        loadClass("com.miui.home.launcher.shortcuts.ShortcutMenuItem").methodFinder().filterByName("getShortTitle")
            .toList().createHooks {
                after { param ->
                    if (param.result.equals("应用信息")) {
                        param.result = "信息"
                    }
                }
            }
        loadClass("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem\$AppDetailsShortcutMenuItem").methodFinder()
            .filterByName("getOnClickListener").toList().createHooks {
                before { param ->
                    when (callMethod(param.thisObject, "getShortTitle")) {
                        EzXHelper.moduleRes.getString(R.string.freeform) -> {
                            param.result = OnClickListener { view ->
                                val context = view.context
                                val componentName = callMethod(param.thisObject, "getComponentName") as ComponentName
                                val intent = Intent().apply {
                                    action = "android.intent.action.MAIN"
                                    addCategory("android.intent.category.DEFAULT")
                                    component = componentName
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                callStaticMethod(
                                    loadClass("com.miui.launcher.utils.ActivityUtilsCompat"),
                                    "makeFreeformActivityOptions",
                                    context,
                                    componentName.packageName
                                )?.let {
                                    context.startActivity(intent, (it as ActivityOptions).toBundle())
                                }
                            }
                        }
                    }
                }
            }
        loadClass("com.miui.home.launcher.shortcuts.SystemShortcutMenu").methodFinder()
            .filterByName("getMaxShortcutItemCount").toList().createHooks {
                after { param ->
                    param.result = 5
                }
            }
        loadClass("com.miui.home.launcher.shortcuts.AppShortcutMenu").methodFinder()
            .filterByName("getMaxShortcutItemCount").toList().createHooks {
                after { param ->
                    param.result = 5
                }
            }
        clazzSystemShortcutMenuItem.methodFinder().filterByName("createAllSystemShortcutMenuItems").toList()
            .createHooks {
                after {
                    @Suppress("UNCHECKED_CAST") val mAllSystemShortcutMenuItems =
                        getStaticObjectField(clazzSystemShortcutMenuItem, "sAllSystemShortcutMenuItems") as List<Any>
                    val mSmallWindowInstance =
                        loadClass("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem\$AppDetailsShortcutMenuItem").newInstance()
                            .apply {
                                callMethod(this, "setShortTitle", EzXHelper.moduleRes.getString(R.string.freeform))
                                callMethod(this, "setIconDrawable", EzXHelper.appContext.let {
                                    it.getDrawable(
                                        it.resources.getIdentifier(
                                            "ic_task_small_window", "drawable", EzXHelper.hostPackageName

                                        )
                                    )
                                })
                            }

                    val sAllSystemShortcutMenuItems = ArrayList<Any>().apply {
                        add(mSmallWindowInstance)
                        addAll(mAllSystemShortcutMenuItems)
                    }
                    setStaticObjectField(
                        clazzSystemShortcutMenuItem, "sAllSystemShortcutMenuItems", sAllSystemShortcutMenuItems
                    )
                }
            }
    }
}