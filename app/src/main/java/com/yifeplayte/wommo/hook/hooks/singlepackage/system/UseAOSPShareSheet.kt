package com.yifeplayte.wommo.hook.hooks.singlepackage.system

import android.provider.Settings
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.HYPER_OS_VERSION
import io.github.lingqiqi5211.ezhooktool.core.findMethod

@Suppress("unused")
object UseAOSPShareSheet : BaseHook() {
    override val key = "use_aosp_share_sheet"
    override val isEnabled = (HYPER_OS_VERSION < 2) && super.isEnabled
    override fun hook() {
        loadClass("com.android.internal.app.ResolverActivityStubImpl").findMethod {
            name("useAospShareSheet")
        }.createHook {
            before {
                it.result = Settings.System.getInt(
                    EzXposed.appContext.contentResolver,
                    "mishare_enabled"
                ) != 1
            }
        }
    }
}