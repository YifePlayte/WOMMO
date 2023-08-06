package com.yifeplayte.wommo.hook.hooks.singlepackage

import com.yifeplayte.wommo.hook.hooks.BasePackage
import com.yifeplayte.wommo.hook.hooks.singlepackage.downloadprovider.RemoveXlDownload

object DownloadProvider : BasePackage() {
    override val packageName = "com.android.providers.downloads"
    override val hooks = setOf(
        RemoveXlDownload,
    )
}