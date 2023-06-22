package com.yifeplayte.wommo.hook

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.yifeplayte.wommo.hook.hooks.multipackage.ForceSupportSendApp
import com.yifeplayte.wommo.hook.hooks.singlepackage.Barrage
import com.yifeplayte.wommo.hook.hooks.singlepackage.Home
import com.yifeplayte.wommo.hook.hooks.singlepackage.PackageInstaller
import com.yifeplayte.wommo.hook.hooks.singlepackage.PersonalAssistant
import com.yifeplayte.wommo.hook.hooks.singlepackage.ScreenRecorder
import com.yifeplayte.wommo.hook.hooks.singlepackage.SecurityCenter
import com.yifeplayte.wommo.hook.hooks.singlepackage.SystemUI
import com.yifeplayte.wommo.hook.hooks.subpackage.SystemUIPlugin
import com.yifeplayte.wommo.hook.utils.DexKit
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

private const val TAG = "WOMMO"
val PACKAGE_NAME_HOOKED = setOf(
    "com.miui.screenrecorder",
    "com.android.systemui",
    "com.miui.home",
    "com.miui.securitycenter",
    "com.miui.packageinstaller",
    "com.miui.personalassistant",
    "com.milink.service",
    "com.xiaomi.mirror",
    "com.xiaomi.barrage",
)

@Suppress("unused")
class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName in PACKAGE_NAME_HOOKED) {

            // init DexKit and EzXHelper
            if (lpparam.packageName != "android") DexKit.initDexKit(lpparam)
            EzXHelper.initHandleLoadPackage(lpparam)
            EzXHelper.setLogTag(TAG)
            EzXHelper.setToastTag(TAG)

            // single package
            setOf(
                Barrage,
                Home,
                PackageInstaller,
                PersonalAssistant,
                ScreenRecorder,
                SecurityCenter,
                SystemUI,
            ).forEach { it.init() }

            // multiple package
            setOf(
                ForceSupportSendApp,
            ).forEach { it.init() }

            // single sub-package
            setOf(
                SystemUIPlugin,
            ).forEach { it.init() }

            DexKit.closeDexKit()
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelper.initZygote(startupParam)
    }
}
