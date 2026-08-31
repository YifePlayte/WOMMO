package com.yifeplayte.wommo.hook.hooks.singlepackage.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.net.wifi.WifiConfiguration
import android.view.View
import android.widget.Button
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.ClipboardUtils.copy
import com.yifeplayte.wommo.hook.utils.hostPackageName
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.newInstanceAuto
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

@Suppress("unused", "DEPRECATION")
@SuppressLint("DiscouragedApi")
object ShowWifiPassword : BaseHook() {
    override val key = "show_wifi_password"
    override val isEnabled = true
    private val idStringWifiSsid by lazy {
        EzXposed.appContext.resources.getIdentifier("wifi_ssid", "string", hostPackageName)
    }
    private val idStringWifiSecurity by lazy {
        EzXposed.appContext.resources.getIdentifier("wifi_security", "string", hostPackageName)
    }
    private val idStringWifiPassword by lazy {
        EzXposed.appContext.resources.getIdentifier("wifi_password", "string", hostPackageName)
    }
    private val idStringWifiDetailsTitle by lazy {
        EzXposed.appContext.resources.getIdentifier("wifi_details_title", "string", hostPackageName)
    }
    private val idStringWifiMenuForget by lazy {
        EzXposed.appContext.resources.getIdentifier("wifi_menu_forget", "string", hostPackageName)
    }
    private val idStringCopy by lazy {
        EzXposed.appContext.resources.getIdentifier("copy", "string", hostPackageName)
    }
    private val idStringPreferenceCopied by lazy {
        EzXposed.appContext.resources.getIdentifier("preference_copied", "string", hostPackageName)
    }
    private val idStyleAlertDialogThemeDayNight by lazy {
        EzXposed.appContext.resources.getIdentifier("AlertDialog_Theme_DayNight", "style", hostPackageName)
    }
    private val idIdBtnDelete by lazy {
        EzXposed.appContext.resources.getIdentifier("btn_delete", "id", hostPackageName)
    }

    override fun hook() {
        loadClass("com.android.settings.wifi.SavedAccessPointPreference").findMethod {
            name("onBindViewHolder")
        }.createHook {
            after { param ->
                val view = param.thisObject.getFieldOrNull("mView") as View
                val button = view.findViewById<Button>(idIdBtnDelete)
                button.setText(idStringWifiDetailsTitle)
            }
        }
        loadClass("com.android.settings.wifi.MiuiSavedAccessPointsWifiSettings").findMethod {
            name("showDeleteDialog")
        }.createHook {
            replace { param ->
                val activity = param.thisObject.callMethod("getActivity") as Activity
                EzXposed.initAppContext(activity)
                val wifiEntry = param.args[0]!!
                val wifiConfig = wifiEntry.callMethod(
                    "getWifiConfiguration"
                ) as WifiConfiguration
                val wifiConfigWithPsk =
                    (loadClass("com.android.settings.wifi.WifiConfigurationManager").callStaticMethod(
                        "getInstance", activity
                    )?.callMethod("getWifiConfigurationWithPsk", wifiConfig)
                        ?: loadClass("com.android.settings.wifi.WifiConfigForSupplicant").callStaticMethod(
                            "getInstance"
                        )?.callMethod(
                            "getWifiConfiguration", wifiConfig, activity
                        ) ?: return@replace null) as WifiConfiguration
                val password =
                    (wifiConfigWithPsk.getFieldOrNull("wepKeys")?.let {
                        @Suppress("UNCHECKED_CAST")
                        (it as Array<String?>)[0]
                    } ?: wifiConfigWithPsk.getFieldOrNullAs<String>("preSharedKey"))?.trim('"')
                val message = buildString {
                    append(activity.getString(idStringWifiSsid))
                    append(": ")
                    append(wifiEntry.callMethod("getTitle"))
                    append("\n")
                    append(activity.getString(idStringWifiSecurity))
                    append(": ")
                    append(
                        wifiEntry.callMethod(
                            "getSecurityString", false
                        )
                    )
                    val isOweTransitionMode = (wifiEntry.callMethod(
                        "getSecurityTypes"
                    ) as List<*>).any { it in setOf(0, 6) }

                    if (!isOweTransitionMode && !password.isNullOrEmpty()) {
                        append("\n")
                        append(activity.getString(idStringWifiPassword))
                        append(": ")
                        append(password)
                    }
                }
                val builder = loadClass("miuix.appcompat.app.AlertDialog\$Builder").newInstanceAuto(
                    activity, idStyleAlertDialogThemeDayNight
                )
                builder.callMethod("setTitle", idStringWifiDetailsTitle)
                builder.callMethod("setMessage", message)
                builder.callMethod("setPositiveButton",
                    idStringWifiMenuForget,
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            param.thisObject.callMethod("deleteSavedConfig", wifiEntry)
                        }
                    })
                builder.callMethod("setNegativeButton",
                    idStringCopy,
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            if (password != null) copy(
                                password,
                                activity.getString(idStringPreferenceCopied, password)
                            )
                        }
                    })
                builder.callMethod("setOnDismissListener",
                    object : DialogInterface.OnDismissListener {
                        override fun onDismiss(dialog: DialogInterface?) {
                            param.thisObject.putField("mIsDismiss", true)
                        }
                    })
                val alertDialog = builder.callMethod("create")!!
                val mIsDismiss =
                    param.thisObject.getFieldOrNullAs<Boolean>("mIsDismiss") == true
                val isOperatorForbidDelSsid = param.thisObject.callMethod(
                    "isOperatorForbidDelSsid", wifiConfig.getFieldOrNullAs<String>("SSID")
                ) as Boolean
                if (mIsDismiss && !isOperatorForbidDelSsid) {
                    param.thisObject.putField("mIsDismiss", false)
                    alertDialog.callMethod("show")
                }
                return@replace null
            }
        }
    }
}
