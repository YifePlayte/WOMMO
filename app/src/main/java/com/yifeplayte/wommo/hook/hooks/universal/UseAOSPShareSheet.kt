package com.yifeplayte.wommo.hook.hooks.universal

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseUniversalHook

object UseAOSPShareSheet : BaseUniversalHook() {
    override val key = "use_aosp_share_sheet"
    override fun hook() {
        loadClass("com.android.internal.app.ResolverActivityStubImpl").methodFinder()
            .filterByName("useAospShareSheet").first().createHook {
                returnConstant(true)
            }
    }
}