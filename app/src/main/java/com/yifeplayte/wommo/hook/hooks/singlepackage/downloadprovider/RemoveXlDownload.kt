package com.yifeplayte.wommo.hook.hooks.singlepackage.downloadprovider

import android.os.Environment
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import java.io.File
import java.io.FileNotFoundException

object RemoveXlDownload : BaseHook() {
    override val key = "remove_xl_download"
    override fun hook() {
        val targetPath = File(Environment.getExternalStorageDirectory(), ".xlDownload").absoluteFile
        File::class.java.methodFinder().filterByName("mkdirs").first().createHook {
            before {
                if ((it.thisObject as File).absoluteFile.equals(targetPath)) {
                    it.throwable = FileNotFoundException("blocked")
                }
            }
        }
    }
}