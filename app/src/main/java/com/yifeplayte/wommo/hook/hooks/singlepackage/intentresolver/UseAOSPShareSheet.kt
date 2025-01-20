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
import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullUntilSuperclass
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.HYPER_OS_VERSION
import org.lsposed.hiddenapibypass.HiddenApiBypass

@Suppress("unused")
object UseAOSPShareSheet : BaseHook() {
    override val key = "use_aosp_share_sheet"
    override val isEnabled = (HYPER_OS_VERSION >= 2) && super.isEnabled

    @SuppressLint("DiscouragedApi")
    override fun hook() {
        Log.i("UseAOSPShareSheet hooking")
        HiddenApiBypass.addHiddenApiExemptions(
            "Lcom/android/internal/R\$id;",
            "Landroid/os/UserManager;",
            "Landroid/content/pm/UserInfo;",
            "Lcom/android/internal/logging/MetricsLogger;"
        )
        val clazzInternalResId = loadClass("com.android.internal.R\$id")
        val clazzMetricsLogger = loadClass("com.android.internal.logging.MetricsLogger")
        val idButtonAlways = getStaticObjectOrNullAs<Int>(clazzInternalResId, "button_always")

        loadClass("com.android.intentresolver.ApplicationStubImpl").methodFinder()
            .filterByName("useAospVersion").single().createHook {
                before {
                    it.result = Settings.System.getInt(
                        appContext.contentResolver, "mishare_enabled"
                    ) != 1
                }
            }

        // Fix the seemingly deleted onButtonClick method for Xiaomi
        loadClass("com.android.intentresolver.ResolverActivity").methodFinder()
            .filterByName("resetButtonBar").single().createHook {
                after { param ->
                    val activity = param.thisObject as Activity
                    val mOnceButton = getObjectOrNullAs<Button>(activity, "mOnceButton")!!
                    val mAlwaysButton = getObjectOrNullAs<Button>(activity, "mAlwaysButton")!!

                    val onClickListener = View.OnClickListener { v ->
                        val mMultiProfilePagerAdapter =
                            getObjectOrNull(activity, "mMultiProfilePagerAdapter")!!
                        val mResolvingHome = getObjectOrNull(activity, "mResolvingHome") as Boolean
                        val mPackageManager =
                            getObjectOrNullAs<PackageManager>(activity, "mPackageManager")!!
                        val mDevicePolicyResources =
                            getObjectOrNull(activity, "mDevicePolicyResources")!!

                        val listView = run {
                            val mCurrentPage =
                                getObjectOrNullUntilSuperclass(
                                    mMultiProfilePagerAdapter,
                                    "mCurrentPage"
                                ) as Int
                            invokeMethodBestMatch(
                                mMultiProfilePagerAdapter, "getListViewForIndex", null, mCurrentPage
                            ) as ListView
                        }
                        val activeListAdapter = invokeMethodBestMatch(
                            mMultiProfilePagerAdapter, "getActiveListAdapter"
                        )!!

                        val hasFilteredItem = invokeMethodBestMatch(
                            activeListAdapter, "hasFilteredItem"
                        ) as Boolean
                        val which = if (hasFilteredItem) {
                            val mFilterLastUsed =
                                getObjectOrNull(activeListAdapter, "mFilterLastUsed") as Boolean
                            val mLastChosenPosition =
                                getObjectOrNull(activeListAdapter, "mLastChosenPosition") as Int
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

                            val target = invokeMethodBestMatch(
                                activeListAdapter,
                                "targetInfoForPosition",
                                null,
                                which,
                                hasIndexBeenFiltered
                            )!!

                            val ri = run resolveInfoForPosition@{
                                target.let {
                                    invokeMethodBestMatch(it, "getResolveInfo") as ResolveInfo
                                }
                            }

                            val hasManagedProfile = run hasManagedProfile@{
                                val userManager =
                                    activity.getSystemService(Context.USER_SERVICE) as UserManager?
                                        ?: return@hasManagedProfile false

                                val userId = runCatching {
                                    invokeMethodBestMatch(
                                        activity, "getUserId"
                                    ) as Int
                                }.getOrElse { return@hasManagedProfile false }

                                val profiles = runCatching {
                                    invokeMethodBestMatch(
                                        userManager, "getProfiles", null, userId
                                    ) as List<*>
                                }.getOrElse { return@hasManagedProfile false }

                                profiles.any { userInfo ->
                                    userInfo != null && runCatching {
                                        invokeMethodBestMatch(
                                            userInfo, "isManagedProfile"
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
                                        val policyResources = getObjectOrNull(
                                            mDevicePolicyResources, "policyResources"
                                        ) as DevicePolicyResourcesManager
                                        val resources = getObjectOrNull(
                                            mDevicePolicyResources, "resources"
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

                            val onTargetSelected = invokeMethodBestMatch(
                                activity, "onTargetSelected", null, target, always
                            ) as Boolean

                            if (onTargetSelected) {
                                if (always) {
                                    invokeStaticMethodBestMatch(
                                        clazzMetricsLogger, "action", null, activity, 455
                                    )
                                } else {
                                    invokeStaticMethodBestMatch(
                                        clazzMetricsLogger, "action", null, activity, 456
                                    )
                                }
                            }

                            if (hasFilteredItem) {
                                invokeStaticMethodBestMatch(
                                    clazzMetricsLogger, "action", null, activity, 452
                                )
                            } else {
                                invokeStaticMethodBestMatch(
                                    clazzMetricsLogger, "action", null, activity, 454
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