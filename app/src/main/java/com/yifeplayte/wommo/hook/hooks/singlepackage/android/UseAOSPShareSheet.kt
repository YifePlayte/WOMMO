package com.yifeplayte.wommo.hook.hooks.singlepackage.android

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object UseAOSPShareSheet : BaseHook() {
    override val key = "use_aosp_share_sheet"
    override fun hook() {
        loadClass("com.android.internal.app.ResolverActivityStubImpl").methodFinder()
            .filterByName("useAospShareSheet").single().createHook {
                returnConstant(true)
            }
    }
}