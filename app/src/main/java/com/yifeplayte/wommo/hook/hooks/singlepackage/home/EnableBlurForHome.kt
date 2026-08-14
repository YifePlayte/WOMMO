package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import java.lang.reflect.Method

@Suppress("unused")
object EnableBlurForHome : BaseHook() {
    override val key = "enable_blur_for_home"
    override fun hook() {
        mutableListOf<Method>().apply {
            addAll(dexKitBridge.findMethod {
                matcher {
                    name(StringMatcher("SupportBlur"))
                    returnType = "boolean"
                }
            }.map { it.getMethodInstance() })
            addAll(dexKitBridge.findMethod {
                matcher {
                    name(StringMatcher("BlurSupported"))
                    returnType = "boolean"
                }
            }.map { it.getMethodInstance() })
        }.createHooks { returnConstant(true) }
    }
}