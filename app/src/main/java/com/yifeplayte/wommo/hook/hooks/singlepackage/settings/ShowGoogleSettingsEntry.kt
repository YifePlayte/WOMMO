package com.yifeplayte.wommo.hook.hooks.singlepackage.settings

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_INTERNATIONAL_BUILD

@Suppress("unused")
object ShowGoogleSettingsEntry : BaseHook() {
    override val key = "show_google_settings_entry"
    override val isEnabled get() = !IS_INTERNATIONAL_BUILD and super.isEnabled
    override fun hook() {
        loadClass("com.android.settings.MiuiSettings").methodFinder()
            .filterByName("updateHeaderList").single().createHook {
                after {
                    if (!IS_INTERNATIONAL_BUILD) invokeMethodBestMatch(
                        it.thisObject, "AddGoogleSettingsHeaders", null, it.args[0]
                    )
                }
            }
    }
}