package com.yifeplayte.wommo.hook.hooks.singlepackage.xiaomiserviceframework

import android.os.Bundle
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import java.lang.reflect.Method


@Suppress("unused")
object ForceAuthSuccess : BaseHook() {
    override val key = "force_auth_success_for_xmsf"
    override fun hook() {
        val clazzAuthSession = loadClass("com.xiaomi.xms.auth.AuthSession")
        // 返回值与 Bundle 双向可赋值（等价旧 filterByAssignableReturnType）
        val returnTypeRelatedToBundle: Method.() -> Boolean = {
            returnType.isAssignableFrom(Bundle::class.java) || Bundle::class.java.isAssignableFrom(returnType)
        }
        val methodOnFailure = clazzAuthSession.findMethod {
            paramCount(1)
            filter(returnTypeRelatedToBundle)
            notAbstract()
        }
        val methodOnSuccess = clazzAuthSession.findMethod {
            paramCount(0)
            filter(returnTypeRelatedToBundle)
            notAbstract()
        }
        methodOnFailure.createHook {
            before {
                it.result = methodOnSuccess.invoke(it.thisObject)
            }
        }
    }
}