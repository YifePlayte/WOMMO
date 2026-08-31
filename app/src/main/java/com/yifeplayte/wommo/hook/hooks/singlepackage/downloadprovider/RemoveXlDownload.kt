package com.yifeplayte.wommo.hook.hooks.singlepackage.downloadprovider

import io.github.lingqiqi5211.ezhooktool.core.findAllMethods
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks

import com.yifeplayte.wommo.hook.hooks.BaseHook
import java.io.IOException

@Suppress("unused")
object RemoveXlDownload : BaseHook() {
    override val key = "remove_xl_download"
    override fun hook() {
        loadClass("com.android.providers.downloads.config.XLConfig").findAllMethods {
            filter { name in setOf("setDebug", "setSoDebug") }; notAbstract()
        }.createHooks {
            returnConstant(null)
        }
        loadClass("com.android.providers.downloads.util.FileUtil").findMethod {
            name("createFile")
        }.createHook {
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