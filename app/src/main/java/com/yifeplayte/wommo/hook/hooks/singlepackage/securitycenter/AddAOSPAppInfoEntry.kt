package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import android.view.Menu
import android.view.MenuItem
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import com.yifeplayte.wommo.hook.utils.hostPackageName
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
@SuppressLint("DiscouragedApi")
object AddAOSPAppInfoEntry : BaseHook() {
    override val key = "add_aosp_app_info_entry"
    private val idIdMiuixActionEndMenuGroup by lazy {
        EzXposed.appContext.resources.getIdentifier("miuix_action_end_menu_group", "id", hostPackageName)
    }
    private val idDrawableIconSettings by lazy {
        EzXposed.appContext.resources.getIdentifier("icon_settings", "drawable", hostPackageName)
    }
    private val idStringAppManagerAppInfoLabel by lazy {
        EzXposed.appContext.resources.getIdentifier("app_manager_app_info_label", "string", hostPackageName)
    }

    override fun hook() {
        val clazzApplicationsDetailsActivity =
            loadClass("com.miui.appmanager.ApplicationsDetailsActivity")
        clazzApplicationsDetailsActivity.findMethod { name("onCreateOptionsMenu") }
            .createHook {
                after {
                    val activity = it.thisObject as Activity
                    EzXposed.initAppContext(activity, true)
                    val pkgName = activity.intent.getStringExtra("package_name")!!
                    val myUserId =
                        UserHandle::class.java.callStaticMethod("myUserId") as Int
                    val uid = activity.intent.getIntExtra("miui.intent.extra.USER_ID", myUserId)
                    val menuItem = (it.args[0] as Menu).add(
                        idIdMiuixActionEndMenuGroup, 0, 0, R.string.aosp_app_info
                    )
                    menuItem.intent = Intent(Intent.ACTION_MAIN).apply {
                        val bundle = Bundle().apply {
                            putString("package", pkgName)
                            putInt("uid", uid)
                        }
                        val stringAppManagerAppInfoLabel =
                            activity.getString(idStringAppManagerAppInfoLabel)
                        setClassName("com.android.settings", "com.android.settings.SubSettings")
                        putExtra(
                            ":settings:show_fragment",
                            "com.android.settings.applications.appinfo.AppInfoDashboardFragment"
                        )
                        putExtra(":settings:show_fragment_title", stringAppManagerAppInfoLabel)
                        putExtra(":settings:show_fragment_args", bundle)
                    }
                    menuItem.setIcon(idDrawableIconSettings)
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
            }
    }
}