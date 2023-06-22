package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.packageinstaller.AllowUnofficialSystemApplicationsInstallation

object PackageInstaller : BasePackage() {
    override val packageName = "com.miui.packageinstaller"
    override val hooks = setOf(
        AllowUnofficialSystemApplicationsInstallation,
    )
}