package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.voiceassist.ChangeBrowserForMiAi

object VoiceAssist : BasePackage() {
    override val packageName = "com.miui.voiceassist"
    override val hooks = setOf(
        ChangeBrowserForMiAi,
    )
}