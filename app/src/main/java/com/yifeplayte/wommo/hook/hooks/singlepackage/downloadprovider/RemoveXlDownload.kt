package com.yifeplayte.wommo.hook.hooks.singlepackage.downloadprovider

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import java.io.IOException

@Suppress("unused")
object RemoveXlDownload : BaseHook() {
    override val key = "remove_xl_download"
    override fun hook() {
        loadClass("com.android.providers.downloads.config.XLConfig").methodFinder()
            .filter { name in setOf("setDebug", "setSoDebug") }
            .filterNonAbstract().toList()
            .createHooks {
                returnConstant(null)
            }
        loadClass("com.android.providers.downloads.util.FileUtil").methodFinder()
            .filterByName("createFile").single().createHook {
                before {
                    if ((it.args[0] as String).contains(".xlDownload")) {
                        it.throwable = IOException(".xlDownload is blocked")
                    }
                }
            }
        // val targetPath = File(Environment.getExternalStorageDirectory(), ".xlDownload").absoluteFile
        // File::class.java.methodFinder().filterByName("mkdirs").single().createHook {
        //     before {
        //         if ((it.thisObject as File).absoluteFile.equals(targetPath)) {
        //             it.throwable = FileNotFoundException("blocked")
        //         }
        //     }
        // }
    }
}