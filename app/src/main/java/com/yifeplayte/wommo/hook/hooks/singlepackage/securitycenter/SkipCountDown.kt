package com.yifeplayte.wommo.hook.hooks.singlepackage.securitycenter

import android.os.Handler
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

object SkipCountDown : BaseHook() {
    override val key = "skip_count_down"
    override fun hook() {
        val mInterceptBaseFragmentCls = loadClass("com.miui.permcenter.privacymanager.InterceptBaseFragment")
        val mInnerClasses = mInterceptBaseFragmentCls.declaredClasses

        loadClass("android.widget.TextView").methodFinder().filterByName("setEnabled").first().createHook {
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
            clazz.methodFinder().filterByAssignableReturnType(Void.TYPE).filterByAssignableParamTypes(Int::class.javaPrimitiveType)
                .first().createHook {
                    before {
                        it.args[0] = 0
                    }
                }
        }
    }
}