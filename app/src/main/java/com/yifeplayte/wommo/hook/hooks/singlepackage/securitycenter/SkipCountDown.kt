package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import android.os.Handler
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object SkipCountDown : BaseHook() {
    override val key = "skip_count_down"
    override fun hook() {
        val mInterceptBaseFragmentCls =
            loadClass("com.miui.permcenter.privacymanager.InterceptBaseFragment")
        val mInnerClasses = mInterceptBaseFragmentCls.declaredClasses

        loadClass("android.widget.TextView").findMethod { name("setEnabled") }
            .createHook {
                before {
                    it.args[0] = true
                }
            }

        mInnerClasses.firstOrNull { Handler::class.java.isAssignableFrom(it) }?.let { clazz ->
            clazz.declaredConstructors.filter { it.parameterCount == 2 }.createHooks {
                before {
                    it.args[1] = 0
                }
            }
            clazz.findMethod {
                voidReturnType()
                paramsAssignableFrom(Int::class.javaPrimitiveType!!)
            }.createHook {
                before {
                    it.args[0] = 0
                }
            }
        }
    }
}