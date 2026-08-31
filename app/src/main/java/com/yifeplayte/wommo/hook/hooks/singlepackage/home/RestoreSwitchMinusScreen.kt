package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.content.Intent
import android.os.Bundle
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putStaticField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

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
        loadClass("com.miui.home.launcher.DeviceConfig").findMethod {
            name("isUseGoogleMinusScreen")
        }.createHook {
            before {
                loadClass("com.miui.home.launcher.LauncherAssistantCompat").putStaticField(
                    "CAN_SWITCH_MINUS_SCREEN",
                    true
                )
            }
        }
        loadClass("com.miui.home.launcher.LauncherAssistantCompat").findMethod {
            name("newInstance")
            paramsAssignableFrom(clazzLauncher)
        }.createHook {
            before {
                val isPersonalAssistantGoogle = (clazzUtilities.callStaticMethod(
                    "getCurrentPersonalAssistant"
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
        clazzLauncher.declaredConstructors.toList().createHooks {
            before {
                if (!IS_INTERNATIONAL_BUILD)
                    setStaticFinalObject(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD", true)
            }
            after {
                setStaticFinalObject(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD", IS_INTERNATIONAL_BUILD)
            }
        }
        clazzMiuiHomeSettings.findMethod {
            name("onCreatePreferences")
            paramsAssignableFrom(Bundle::class.java, String::class.java)
        }.createHook {
            after { param ->
                val mSwitchPersonalAssistant =
                    param.thisObject.getFieldOrNull("mSwitchPersonalAssistant")!!
                mSwitchPersonalAssistant.callMethod(
                    "setIntent",
                    Intent("com.miui.home.action.LAUNCHER_PERSONAL_ASSISTANT_SETTING")
                )
                mSwitchPersonalAssistant.callMethod(
                    "setOnPreferenceChangeListener",
                    param.thisObject
                )
                param.thisObject.callMethod("getPreferenceScreen")!!.callMethod(
                    "addPreference", mSwitchPersonalAssistant
                )
            }
        }
        clazzMiuiHomeSettings.findMethod { name("onResume") }.createHook {
            after { param ->
                param.thisObject.getFieldOrNull("mSwitchPersonalAssistant")!!.callMethod(
                    "setVisible", true
                )
            }
        }
    }
}
