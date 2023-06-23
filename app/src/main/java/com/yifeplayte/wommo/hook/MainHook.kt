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
private val singlePackagesHooked = setOf(
    Barrage,
    Home,
    PackageInstaller,
    PersonalAssistant,
    ScreenRecorder,
    SecurityCenter,
    SystemUI,
)
private val multiPackagesHooked = setOf(
    ForceSupportSendApp,
)
private val subPackagesHooked = setOf(
    SystemUIPlugin,
)
val PACKAGE_NAME_HOOKED: Set<String>
    get() {
        val packageNameHooked = mutableSetOf<String>()
        singlePackagesHooked.forEach { packageNameHooked.add(it.packageName) }
        multiPackagesHooked.forEach { packageNameHooked.addAll(it.hooks.keys) }
        subPackagesHooked.forEach { packageNameHooked.add(it.packageName) }
        return packageNameHooked
    }

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
            singlePackagesHooked.forEach { it.init() }

            // multiple package
            multiPackagesHooked.forEach { it.init() }

            // single sub-package
            subPackagesHooked.forEach { it.init() }

            DexKit.closeDexKit()
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelper.initZygote(startupParam)
    }
}
