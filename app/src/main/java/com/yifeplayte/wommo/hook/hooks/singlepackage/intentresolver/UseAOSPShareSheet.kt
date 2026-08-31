package com.yifeplayte.wommo.hook.hooks.singlepackage.intentresolver

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyResourcesManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.os.UserManager
import android.provider.Settings
import android.view.View
import android.widget.AbsListView
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.getStaticFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.HYPER_OS_VERSION
import org.lsposed.hiddenapibypass.HiddenApiBypass

@Suppress("unused")
object UseAOSPShareSheet : BaseHook() {
    override val key = "use_aosp_share_sheet"
    override val isEnabled = (HYPER_OS_VERSION >= 2) && super.isEnabled

    @SuppressLint("DiscouragedApi")
    override fun hook() {
        HiddenApiBypass.addHiddenApiExemptions(
            $$"Lcom/android/internal/R$id;",
            "Landroid/os/UserManager;",
            "Landroid/content/pm/UserInfo;",
            "Lcom/android/internal/logging/MetricsLogger;"
        )
        val clazzInternalResId = loadClass($$"com.android.internal.R$id")
        val clazzMetricsLogger = loadClass("com.android.internal.logging.MetricsLogger")
        val idButtonAlways = clazzInternalResId.getStaticFieldOrNullAs<Int>("button_always")

        loadClass("com.android.intentresolver.ApplicationStubImpl").findMethod {
            name("useAospVersion")
        }.createHook {
            before {
                it.result = Settings.System.getInt(
                    EzXposed.appContext.contentResolver, "mishare_enabled"
                ) != 1
            }
        }

        // Fix the seemingly deleted onButtonClick method for Xiaomi
        loadClass("com.android.intentresolver.ResolverActivity").findMethod {
            name("resetButtonBar")
        }.createHook {
            after { param ->
                val activity = param.thisObject as Activity
                val mOnceButton = activity.getFieldOrNullAs<Button>("mOnceButton")!!
                val mAlwaysButton = activity.getFieldOrNullAs<Button>("mAlwaysButton")!!

                val onClickListener = View.OnClickListener { v ->
                    val mMultiProfilePagerAdapter =
                        activity.getFieldOrNull("mMultiProfilePagerAdapter")!!
                    val mResolvingHome = activity.getFieldOrNull("mResolvingHome") as Boolean
                    val mPackageManager =
                        activity.getFieldOrNullAs<PackageManager>("mPackageManager")!!
                    val mDevicePolicyResources =
                        activity.getFieldOrNull("mDevicePolicyResources")!!

                    val listView = run {
                        val mCurrentPage =
                            mMultiProfilePagerAdapter.getFieldOrNull(
                                "mCurrentPage"
                            ) as Int
                        mMultiProfilePagerAdapter.callMethod(
                            "getListViewForIndex", mCurrentPage
                        ) as ListView
                    }
                    val activeListAdapter = mMultiProfilePagerAdapter.callMethod(
                        "getActiveListAdapter"
                    )!!

                    val hasFilteredItem = activeListAdapter.callMethod(
                        "hasFilteredItem"
                    ) as Boolean
                    val which = if (hasFilteredItem) {
                        val mFilterLastUsed =
                            activeListAdapter.getFieldOrNull("mFilterLastUsed") as Boolean
                        val mLastChosenPosition =
                            activeListAdapter.getFieldOrNull("mLastChosenPosition") as Int
                        if (mFilterLastUsed && (mLastChosenPosition >= 0)) {
                            mLastChosenPosition
                        } else {
                            AbsListView.INVALID_POSITION
                        }
                    } else {
                        listView.checkedItemPosition
                    }
                    val always = v.id == idButtonAlways
                    val hasIndexBeenFiltered = !hasFilteredItem

                    run startSelected@{
                        if (activity.isFinishing) return@startSelected

                        val target = activeListAdapter.callMethod(
                            "targetInfoForPosition",
                            which,
                            hasIndexBeenFiltered
                        )!!

                        val ri = run resolveInfoForPosition@{
                            target.callMethod("getResolveInfo") as ResolveInfo
                        }

                        val hasManagedProfile = run hasManagedProfile@{
                            val userManager =
                                activity.getSystemService(Context.USER_SERVICE) as? UserManager?
                                    ?: return@hasManagedProfile false

                            val userId = runCatching {
                                activity.callMethod(
                                    "getUserId"
                                ) as Int
                            }.getOrElse { return@hasManagedProfile false }

                            val profiles = runCatching {
                                userManager.callMethod(
                                    "getProfiles", userId
                                ) as List<*>
                            }.getOrElse { return@hasManagedProfile false }

                            profiles.any { userInfo ->
                                userInfo != null && runCatching {
                                    userInfo.callMethod(
                                        "isManagedProfile"
                                    ) as Boolean
                                }.getOrElse { false }
                            }
                        }

                        val supportsManagedProfiles = run supportsManagedProfiles@{
                            runCatching {
                                mPackageManager.getApplicationInfo(
                                    ri.activityInfo.packageName, 0
                                ).targetSdkVersion >= android.os.Build.VERSION_CODES.LOLLIPOP
                            }.getOrDefault(false)
                        }

                        if (mResolvingHome && hasManagedProfile && !supportsManagedProfiles) {
                            val launcherName =
                                ri.activityInfo.loadLabel(mPackageManager).toString()
                            Toast.makeText(
                                activity, run getWorkProfileNotSupportedMessage@{
                                    val policyResources = mDevicePolicyResources.getFieldOrNull(
                                        "policyResources"
                                    ) as DevicePolicyResourcesManager
                                    val resources = mDevicePolicyResources.getFieldOrNull(
                                        "resources"
                                    ) as Resources
                                    requireNotNull(
                                        policyResources.getString(
                                            "Core.RESOLVER_WORK_PROFILE_NOT_SUPPORTED", {
                                                resources.getString(
                                                    resources.getIdentifier(
                                                        "activity_resolver_work_profiles_support",
                                                        "string",
                                                        "com.android.intentresolver"
                                                    ), launcherName
                                                )
                                            }, launcherName
                                        )
                                    )
                                }, Toast.LENGTH_LONG
                            ).show()
                            return@startSelected
                        }

                        val onTargetSelected = activity.callMethod(
                            "onTargetSelected", target, always
                        ) as Boolean

                        if (onTargetSelected) {
                            if (always) {
                                clazzMetricsLogger.callStaticMethod(
                                    "action", activity, 455
                                )
                            } else {
                                clazzMetricsLogger.callStaticMethod(
                                    "action", activity, 456
                                )
                            }
                        }

                        if (hasFilteredItem) {
                            clazzMetricsLogger.callStaticMethod(
                                "action", activity, 452
                            )
                        } else {
                            clazzMetricsLogger.callStaticMethod(
                                "action", activity, 454
                            )
                        }

                        activity.finish()
                    }
                }

                mOnceButton.setOnClickListener(onClickListener)
                mAlwaysButton.setOnClickListener(onClickListener)
            }
        }
    }
}
