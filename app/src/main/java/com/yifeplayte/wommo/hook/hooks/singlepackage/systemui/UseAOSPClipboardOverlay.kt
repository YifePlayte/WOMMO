package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import android.content.ClipboardManager
import com.yifeplayte.wommo.hook.hooks.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.getStaticFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

@Suppress("unused")
object UseAOSPClipboardOverlay : BaseHook() {
    override val key = "use_aosp_clipboard_overlay"
    override fun hook() {
        val clazzClipboardListener =
            loadClass("com.android.systemui.clipboardoverlay.ClipboardListener")
        if (clazzClipboardListener.declaredFields.any { it.name == "sCtsTestPkgList" }) {
            clazzClipboardListener.findMethod {
                name("onPrimaryClipChanged"); notAbstract()
            }.createHook {
                before { param ->
                    val mClipboardManager = runCatching {
                        param.thisObject.getFieldOrNullAs<ClipboardManager>("mClipboardManager")!!
                    }.getOrElse {
                        param.thisObject.getFieldOrNullAs<ClipboardManager>("mClipboardManagerForUser")!!
                    }
                    val primaryClipSource =
                        mClipboardManager.callMethod("getPrimaryClipSource") as String
                    val oldList =
                        param.thisObject.javaClass.getStaticFieldOrNullAs<MutableList<String>>("sCtsTestPkgList")!!
                    oldList[0] = primaryClipSource
                }
            }
        } else {
            clazzClipboardListener.findMethod {
                name("start"); notAbstract()
            }.createHook {
                before {
                    val mClipboardManager =
                        it.thisObject.getFieldOrNullAs<ClipboardManager>("mClipboardManager")!!
                    @Suppress("UNCHECKED_CAST")
                    mClipboardManager.addPrimaryClipChangedListener(
                        it.thisObject as? ClipboardManager.OnPrimaryClipChangedListener
                    )
                    it.result = null
                }
            }
        }
    }
}
