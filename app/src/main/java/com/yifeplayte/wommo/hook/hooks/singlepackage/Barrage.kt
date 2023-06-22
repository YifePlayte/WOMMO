package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.barrage.ForceSupportBarrage

object Barrage : BasePackage() {
    override val packageName = "com.xiaomi.barrage"
    override val hooks = setOf(
        ForceSupportBarrage,
    )
}