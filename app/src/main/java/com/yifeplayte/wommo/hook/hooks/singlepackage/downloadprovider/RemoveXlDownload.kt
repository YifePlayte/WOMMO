package com.yifeplayte.wommo.hook.hooks.singlepackage.downloadprovider

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook

object RemoveXlDownload : BaseHook() {
    override val key = "remove_xl_download"
    override fun hook() {
        val clazzXLConfig = loadClass("com.android.providers.downloads.config.XLConfig")
        clazzXLConfig.methodFinder().filter { name in setOf("setDebug", "setSoDebug") }.toList().createHooks {
            returnConstant(null)
        }
        // val targetPath = File(Environment.getExternalStorageDirectory(), ".xlDownload").absoluteFile
        // File::class.java.methodFinder().filterByName("mkdirs").first().createHook {
        //     before {
        //         if ((it.thisObject as File).absoluteFile.equals(targetPath)) {
        //             it.throwable = FileNotFoundException("blocked")
        //         }
        //     }
        // }
    }
}