package com.yifeplayte.wommo.hook.hooks.singlepackage.android

import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object UseAOSPScreenshot : BaseHook() {
    override val key = "use_aosp_screenshot"
    override fun hook() {
        invokeStaticMethodBestMatch(
            loadClass("com.android.internal.util.ScreenshotHelperStub"), "getInstance"
        )?.let {
            it::class.java.methodFinder().filterByName("getServiceComponent").filterNonAbstract()
                .toList().createHooks {
                    returnConstant("com.android.systemui/com.android.systemui.screenshot.TakeScreenshotService")
                }
        }
    }
}