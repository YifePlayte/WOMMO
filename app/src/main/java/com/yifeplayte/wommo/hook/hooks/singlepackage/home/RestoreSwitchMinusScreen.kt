package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.content.Intent
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD
import com.yifeplayte.wommo.utils.Clazz.setStaticFinalObject

@Suppress("unused")
object RestoreSwitchMinusScreen : BaseHook() {
    override val key = "restore_switch_minus_screen"
    override fun hook() {
        val clazzUtilities = loadClass("com.miui.home.launcher.common.Utilities")
        val clazzMiuiBuild = loadClass("miui.os.Build")
        val clazzLauncher = loadClass("com.miui.home.launcher.Launcher")
        val clazzMiuiHomeSettings = loadClass("com.miui.home.settings.MiuiHomeSettings")
        loadClass("com.miui.home.launcher.DeviceConfig").methodFinder()
            .filterByName("isUseGoogleMinusScreen").single()
            .createHook {
                before {
                    setStaticObject(
                        loadClass("com.miui.home.launcher.LauncherAssistantCompat"),
                        "CAN_SWITCH_MINUS_SCREEN",
                        true
                    )
                }
            }
        loadClass("com.miui.home.launcher.LauncherAssistantCompat").methodFinder()
            .filterByName("newInstance")
            .filterByAssignableParamTypes(clazzLauncher).single().createHook {
                before {
                    val isPersonalAssistantGoogle = (invokeStaticMethodBestMatch(
                        clazzUtilities, "getCurrentPersonalAssistant"
                    )!! as String) == "personal_assistant_google"
                    if (IS_INTERNATIONAL_BUILD != isPersonalAssistantGoogle)
                        setStaticFinalObject(
                            clazzMiuiBuild,
                            "IS_INTERNATIONAL_BUILD",
                            isPersonalAssistantGoogle
                        )
                }
                after {
                    setStaticFinalObject(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD", IS_INTERNATIONAL_BUILD)
                }
            }
        clazzLauncher.declaredConstructors.createHooks {
            before {
                if (!IS_INTERNATIONAL_BUILD)
                    setStaticFinalObject(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD", true)
            }
            after {
                setStaticFinalObject(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD", IS_INTERNATIONAL_BUILD)
            }
        }
        clazzMiuiHomeSettings.methodFinder().filterByName("onCreatePreferences")
            .filterByAssignableParamTypes(Bundle::class.java, String::class.java).single()
            .createHook {
                after { param ->
                    val mSwitchPersonalAssistant =
                        getObjectOrNull(param.thisObject, "mSwitchPersonalAssistant")!!
                    mSwitchPersonalAssistant.objectHelper {
                        invokeMethodBestMatch(
                            "setIntent",
                            null,
                            Intent("com.miui.home.action.LAUNCHER_PERSONAL_ASSISTANT_SETTING")
                        )
                        invokeMethodBestMatch(
                            "setOnPreferenceChangeListener",
                            null,
                            param.thisObject
                        )
                    }
                    invokeMethodBestMatch(param.thisObject, "getPreferenceScreen")!!.objectHelper()
                        .invokeMethodBestMatch("addPreference", null, mSwitchPersonalAssistant)
                }
            }
        clazzMiuiHomeSettings.methodFinder().filterByName("onResume").single().createHook {
            after { param ->
                getObjectOrNull(param.thisObject, "mSwitchPersonalAssistant")!!.objectHelper()
                    .invokeMethodBestMatch("setVisible", null, true)
            }
        }
    }
}