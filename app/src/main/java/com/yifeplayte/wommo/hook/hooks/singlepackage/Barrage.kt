package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.barrage.ForceSupportBarrage
import com.yifeplayte.wommo.hook.hooks.singlepackage.barrage.GlobalBarrage
import com.yifeplayte.wommo.hook.hooks.singlepackage.barrage.ModifyBarrageLength

object Barrage : BasePackage() {
    override val packageName = "com.xiaomi.barrage"
    override val hooks = setOf(
        ForceSupportBarrage,
        GlobalBarrage,
        ModifyBarrageLength,
    )
}