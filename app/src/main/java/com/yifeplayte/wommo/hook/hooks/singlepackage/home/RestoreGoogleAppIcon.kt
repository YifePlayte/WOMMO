package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.content.ComponentName
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.yifeplayte.wommo.hook.hooks.BaseSingleHook

object RestoreGoogleAppIcon : BaseSingleHook() {
    override fun init() {
        loadClass("com.miui.home.launcher.AppFilter").declaredConstructors.createHooks {
            after { param ->
                getObjectOrNullAs<HashSet<ComponentName>>(param.thisObject, "mSkippedItems")!!.removeIf {
                    it.packageName in setOf("com.google.android.googlequicksearchbox", "com.google.android.gms")
                }
            }
        }
    }
}