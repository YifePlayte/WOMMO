package com.yifeplayte.wommo.hook.hooks.singlepackage.system

import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

import com.yifeplayte.wommo.hook.hooks.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findAllMethods

@Suppress("unused")
object UseAOSPScreenshot : BaseHook() {
    override val key = "use_aosp_screenshot"
    override fun hook() {
        loadClass("com.android.internal.util.ScreenshotHelperStub").callStaticMethod("getInstance")?.let {
            it::class.java.findAllMethods { name("getServiceComponent"); notAbstract() }
                .createHooks {
                    returnConstant("com.android.systemui/com.android.systemui.screenshot.TakeScreenshotService")
                }
        }
    }
}