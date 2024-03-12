package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import android.content.ClipboardManager
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

@Suppress("unused")
object UseAOSPClipboardOverlay : BaseHook() {
    override val key = "use_aosp_clipboard_overlay"
    override fun hook() {
        val clazzClipboardListener =
            loadClass("com.android.systemui.clipboardoverlay.ClipboardListener")
        if (clazzClipboardListener.declaredFields.any {
                it.name == "sCtsTestPkgList"
            }) clazzClipboardListener.methodFinder().filterByName("onPrimaryClipChanged")
            .filterNonAbstract().single().createHook {
                before {
                    val mClipboardManager =
                        getObjectOrNullAs<ClipboardManager>(it.thisObject, "mClipboardManager")!!
                    val primaryClipSource =
                        invokeMethodBestMatch(mClipboardManager, "getPrimaryClipSource") as String
                    val oldList =
                        getObjectOrNullAs<List<String>>(it.thisObject, "sCtsTestPkgList")!!
                    val newList = mutableListOf<String>().apply {
                        addAll(oldList)
                        if (!contains(primaryClipSource)) add(primaryClipSource)
                    }
                    setObject(it.thisObject, "sCtsTestPkgList", newList)
                }
            }
        else clazzClipboardListener.methodFinder().filterByName("start").filterNonAbstract()
            .single().createHook {
                before {
                    val mClipboardManager =
                        getObjectOrNullAs<ClipboardManager>(it.thisObject, "mClipboardManager")!!
                    mClipboardManager.addPrimaryClipChangedListener(it.thisObject as ClipboardManager.OnPrimaryClipChangedListener?)
                    it.result = null
                }
            }
    }
}