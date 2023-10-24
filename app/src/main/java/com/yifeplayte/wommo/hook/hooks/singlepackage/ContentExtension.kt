package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.contentextension.ChangeBrowserForContentExtension

object ContentExtension : BasePackage() {
    override val packageName = "com.miui.contentextension"
    override val hooks = setOf(
        ChangeBrowserForContentExtension,
    )
}