package com.yifeplayte.wommo.hook.hooks.singlepackage.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.net.wifi.WifiConfiguration
import android.view.View
import android.widget.Button
import com.github.kyuubiran.ezxhelper.ClassHelper.Companion.classHelper
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.newInstanceBestMatch
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.EzXHelper.initAppContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.ClipboardUtils.copy
import de.robv.android.xposed.XposedHelpers


@Suppress("unused", "DEPRECATION")
@SuppressLint("DiscouragedApi")
object ShowWifiPassword : BaseHook() {
    override val key = "show_wifi_password"
    override val isEnabled = true
    private val idStringWifiSsid by lazy {
        appContext.resources.getIdentifier("wifi_ssid", "string", hostPackageName)
    }
    private val idStringWifiSecurity by lazy {
        appContext.resources.getIdentifier("wifi_security", "string", hostPackageName)
    }
    private val idStringWifiPassword by lazy {
        appContext.resources.getIdentifier("wifi_password", "string", hostPackageName)
    }
    private val idStringWifiDetailsTitle by lazy {
        appContext.resources.getIdentifier("wifi_details_title", "string", hostPackageName)
    }
    private val idStringWifiMenuForget by lazy {
        appContext.resources.getIdentifier("wifi_menu_forget", "string", hostPackageName)
    }
    private val idStringCopy by lazy {
        appContext.resources.getIdentifier("copy", "string", hostPackageName)
    }
    private val idStringPreferenceCopied by lazy {
        appContext.resources.getIdentifier("preference_copied", "string", hostPackageName)
    }
    private val idStyleAlertDialogThemeDayNight by lazy {
        appContext.resources.getIdentifier("AlertDialog_Theme_DayNight", "style", hostPackageName)
    }
    private val idIdBtnDelete by lazy {
        appContext.resources.getIdentifier("btn_delete", "id", hostPackageName)
    }

    override fun hook() {
        loadClass("com.android.settings.wifi.SavedAccessPointPreference").methodFinder()
            .filterByName("onBindViewHolder").single().createHook {
                after { param ->
                    val view = XposedHelpers.getObjectField(param.thisObject, "mView") as View
                    val button = view.findViewById<Button>(idIdBtnDelete)
                    button.setText(idStringWifiDetailsTitle)
                }
            }
        loadClass("com.android.settings.wifi.MiuiSavedAccessPointsWifiSettings").methodFinder()
            .filterByName("showDeleteDialog").single().createHook {
                replace { param ->
                    val activity =
                        invokeMethodBestMatch(param.thisObject, "getActivity") as Activity
                    initAppContext(activity)
                    val wifiEntry = param.args[0]
                    val wifiConfig = invokeMethodBestMatch(
                        wifiEntry, "getWifiConfiguration"
                    ) as WifiConfiguration
                    val wifiConfigWithPsk =
                        (loadClass("com.android.settings.wifi.WifiConfigurationManager").classHelper()
                            .invokeStaticMethodBestMatch(
                                "getInstance", null, activity
                            )?.objectHelper()
                            ?.invokeMethodBestMatch("getWifiConfigurationWithPsk", null, wifiConfig)
                            ?: loadClass("com.android.settings.wifi.WifiConfigForSupplicant").classHelper()
                                .invokeStaticMethodBestMatch("getInstance")?.objectHelper()
                                ?.invokeMethodBestMatch(
                                    "getWifiConfiguration", null, wifiConfig, activity
                                ) ?: return@replace null) as WifiConfiguration
                    val password =
                        (wifiConfigWithPsk.wepKeys?.get(0) ?: wifiConfigWithPsk.preSharedKey)?.trim('"')
                    val message = buildString {
                        append(activity.getString(idStringWifiSsid))
                        append(": ")
                        append(invokeMethodBestMatch(wifiEntry, "getTitle"))
                        append("\n")
                        append(activity.getString(idStringWifiSecurity))
                        append(": ")
                        append(
                            invokeMethodBestMatch(
                                wifiEntry, "getSecurityString", null, false
                            )
                        )
                        val isOweTransitionMode = (invokeMethodBestMatch(
                            wifiEntry, "getSecurityTypes"
                        ) as List<*>).any { it in setOf(0, 6) }

                        if (!isOweTransitionMode && !password.isNullOrEmpty()) {
                            append("\n")
                            append(activity.getString(idStringWifiPassword))
                            append(": ")
                            append(password)
                        }
                    }
                    val builder = newInstanceBestMatch(
                        loadClass("miuix.appcompat.app.AlertDialog\$Builder"),
                        activity,
                        idStyleAlertDialogThemeDayNight
                    )
                    builder.objectHelper {
                        invokeMethodBestMatch("setTitle", null, idStringWifiDetailsTitle)
                        invokeMethodBestMatch("setMessage", null, message)
                        invokeMethodBestMatch("setPositiveButton",
                            null,
                            idStringWifiMenuForget,
                            object : DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    invokeMethodBestMatch(
                                        param.thisObject, "deleteSavedConfig", null, wifiEntry
                                    )
                                }
                            })
                        invokeMethodBestMatch("setNegativeButton",
                            null,
                            idStringCopy,
                            object : DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    if (password != null) copy(
                                        password,
                                        activity.getString(idStringPreferenceCopied, password)
                                    )
                                }
                            })
                        invokeMethodBestMatch("setOnDismissListener",
                            null,
                            object : DialogInterface.OnDismissListener {
                                override fun onDismiss(dialog: DialogInterface?) {
                                    setObject(param.thisObject, "mIsDismiss", true)
                                }
                            })
                    }
                    val alertDialog = invokeMethodBestMatch(builder, "create")!!
                    val mIsDismiss =
                        getObjectOrNullAs<Boolean>(param.thisObject, "mIsDismiss") == true
                    val isOperatorForbidDelSsid = invokeMethodBestMatch(
                        param.thisObject, "isOperatorForbidDelSsid", null, wifiConfig.SSID
                    ) as Boolean
                    if (mIsDismiss && !isOperatorForbidDelSsid) {
                        setObject(param.thisObject, "mIsDismiss", false)
                        invokeMethodBestMatch(alertDialog, "show")
                    }
                    return@replace null
                }
            }
    }
}