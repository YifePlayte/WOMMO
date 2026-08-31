package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import com.yifeplayte.wommo.hook.utils.hostPackageName
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
@SuppressLint("DiscouragedApi")
object AddAOSPAppManagerEntry : BaseHook() {
    override val key = "add_aosp_app_manager_entry"
    private val idIdMiuixActionEndMenuGroup by lazy {
        EzXposed.appContext.resources.getIdentifier("miuix_action_end_menu_group", "id", hostPackageName)
    }
    private val idDrawableIconSettings by lazy {
        EzXposed.appContext.resources.getIdentifier("icon_settings", "drawable", hostPackageName)
    }

    override fun hook() {
        val clazzAppManagerMainActivity = loadClass("com.miui.appmanager.AppManagerMainActivity")
        clazzAppManagerMainActivity.findMethod { name("onCreateOptionsMenu") }
            .createHook {
                after {
                    EzXposed.initAppContext(it.thisObject as Activity, true)
                    val menuItem = (it.args[0] as Menu).add(
                        idIdMiuixActionEndMenuGroup, 0, 0, R.string.aosp_app_manager
                    )
                    menuItem.intent = Intent(Intent.ACTION_MAIN).setClassName(
                        "com.android.settings",
                        "com.android.settings.applications.ManageApplications"
                    )
                    menuItem.setIcon(idDrawableIconSettings)
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
            }
    }
}