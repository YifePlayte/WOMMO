package com.yifeplayte.wommo.hook.hooks.singlepackage.android

import android.provider.Settings
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.HYPER_OS_VERSION

@Suppress("unused")
object UseAOSPShareSheet : BaseHook() {
    override val key = "use_aosp_share_sheet"
    override val isEnabled = (HYPER_OS_VERSION < 2) && super.isEnabled
    override fun hook() {
        loadClass("com.android.internal.app.ResolverActivityStubImpl").methodFinder()
            .filterByName("useAospShareSheet").single().createHook {
                before {
                    it.result = Settings.System.getInt(
                        appContext.contentResolver,
                        "mishare_enabled"
                    ) != 1
                }
            }
    }
}