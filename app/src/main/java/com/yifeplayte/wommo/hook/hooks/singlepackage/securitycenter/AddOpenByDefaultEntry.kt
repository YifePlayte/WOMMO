package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.net.toUri
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findConstructor
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import com.yifeplayte.wommo.hook.utils.hostPackageName
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.AdditionalFields

@Suppress("unused")
@SuppressLint("DiscouragedApi")
object AddOpenByDefaultEntry : BaseHook() {
    override val key = "add_open_by_default_entry"
    private val domainVerificationManager: DomainVerificationManager by lazy {
        EzXposed.appContext.getSystemService(
            DomainVerificationManager::class.java
        )
    }
    private val idAmDetailDefault by lazy {
        EzXposed.appContext.resources.getIdentifier("am_detail_default", "id", hostPackageName)
    }
    private val idAmDetailDefaultTitle by lazy {
        EzXposed.appContext.resources.getIdentifier("am_detail_default_title", "id", hostPackageName)
    }
    private val drawableAmCardBgSelector by lazy {
        EzXposed.appContext.resources.getIdentifier("am_card_bg_selector", "drawable", hostPackageName)
    }
    private val dimenAmDetailsItemHeight by lazy {
        EzXposed.appContext.resources.getIdentifier("am_details_item_height", "dimen", hostPackageName)
    }
    private val dimenAmMainPageMarginSe by lazy {
        EzXposed.appContext.resources.getIdentifier("am_main_page_margin_se", "dimen", hostPackageName)
    }

    override fun hook() {
        val clazzApplicationsDetailsActivity =
            loadClass("com.miui.appmanager.ApplicationsDetailsActivity")
        clazzApplicationsDetailsActivity.findMethod { name("initView") }
            .createHook {
                after { param ->
                    val activity = param.thisObject as Activity
                    EzXposed.initAppContext(activity, true)
                    var cleanOpenByDefaultView: View? = activity.findViewById(idAmDetailDefault)
                    if (cleanOpenByDefaultView == null) {
                        val viewAmDetailDefaultTitle =
                            activity.findViewById<View>(idAmDetailDefaultTitle)
                        val linearLayout = viewAmDetailDefaultTitle.parent as LinearLayout
                        cleanOpenByDefaultView =
                            (loadClass("com.miui.appmanager.widget.AppDetailBannerItemView").findConstructor {
                                paramCount(2)
                            }.newInstance(activity, null) as LinearLayout).apply {
                                gravity = Gravity.CENTER_VERTICAL
                                orientation = LinearLayout.HORIZONTAL
                                setBackgroundResource(drawableAmCardBgSelector)
                                isClickable = true
                                minimumHeight = activity.resources.getDimensionPixelSize(
                                    dimenAmDetailsItemHeight
                                )
                                val dimensionPixelSize =
                                    activity.resources.getDimensionPixelSize(dimenAmMainPageMarginSe)
                                setPadding(dimensionPixelSize, 0, dimensionPixelSize, 0)
                            }
                        cleanOpenByDefaultView.setOnClickListener {
                            startActionAppOpenByDefaultSettings(activity)
                        }
                        linearLayout.addView(cleanOpenByDefaultView)
                    }
                    AdditionalFields.set(
                        activity, "cleanOpenByDefaultView", cleanOpenByDefaultView
                    )
                    val pkgName = activity.intent.getStringExtra("package_name")!!
                    val isLinkHandlingAllowed =
                        domainVerificationManager.getDomainVerificationUserState(
                            pkgName
                        )?.isLinkHandlingAllowed ?: false
                    cleanOpenByDefaultView.callMethod(
                        "setTitle", R.string.open_by_default
                    )
                    cleanOpenByDefaultView.callMethod(
                        "setSummary",
                        if (isLinkHandlingAllowed) R.string.app_link_open_always else R.string.app_link_open_never
                    )
                }
            }
        clazzApplicationsDetailsActivity.findMethod { name("onClick") }
            .createHook {
                before { param ->
                    val activity = param.thisObject as Activity
                    EzXposed.initAppContext(activity, true)
                    val clickedView = param.args[0]
                    val cleanOpenByDefaultView =
                        AdditionalFields.get(activity, "cleanOpenByDefaultView")
                    if (clickedView == cleanOpenByDefaultView) {
                        startActionAppOpenByDefaultSettings(activity)
                        param.result = null
                    }
                }
            }
    }

    private fun startActionAppOpenByDefaultSettings(activity: Activity) {
        val pkgName = activity.intent.getStringExtra("package_name")!!
        val intent = Intent().apply {
            action = android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
            addCategory(Intent.CATEGORY_DEFAULT)
            data = "package:${pkgName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        activity.startActivity(intent)
    }
}