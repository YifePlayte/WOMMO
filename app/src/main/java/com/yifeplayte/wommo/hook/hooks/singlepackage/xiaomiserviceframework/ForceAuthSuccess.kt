package com.yifeplayte.wommo.hook.hooks.singlepackage.xiaomiserviceframework

import android.os.Bundle
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook


@Suppress("unused")
object ForceAuthSuccess : BaseHook() {
    override val key = "force_auth_success_for_xmsf"
    override fun hook() {
        val clazzAuthSession = loadClass("com.xiaomi.xms.auth.AuthSession")
        val methodOnFailure = clazzAuthSession.methodFinder()
            .filterByParamCount(1)
            .filterByAssignableReturnType(Bundle::class.java)
            .filterNonAbstract()
            .single()
        val methodOnSuccess = clazzAuthSession.methodFinder()
            .filterByParamCount(0)
            .filterByAssignableReturnType(Bundle::class.java)
            .filterNonAbstract()
            .single()
        methodOnFailure.createHook {
            before {
                it.result = methodOnSuccess.invoke(it.thisObject)
            }
        }
    }
}