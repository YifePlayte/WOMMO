package com.yifeplayte.wommo.hook.hooks.home

import android.content.ComponentName
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.yifeplayte.wommo.hook.hooks.BaseHook

object RestoreGoogleAppIcon : BaseHook() {
    override fun init() {
        loadClass("com.miui.home.launcher.AppFilter").constructors.createHooks {
            after { param ->
                param.thisObject.objectHelper {
                    getObjectOrNullAs<HashSet<ComponentName>>("mSkippedItems")?.removeIf {
                        it.packageName in listOf(
                            "com.google.android.googlequicksearchbox",
                            "com.google.android.gms"
                        )
                    }
                }
            }
        }
    }
}