package com.yifeplayte.wommo.hook.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.github.kyuubiran.ezxhelper.AndroidLogger
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.Log

object ClipboardUtils {
    @JvmStatic
    fun copy(string: String, toast: String? = null) {
        val clipboardManager =
            appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData =
            ClipData.newPlainText(Log.currentLogger.toastTag, string)
        clipboardManager.setPrimaryClip(clipData)
        if (toast != null) AndroidLogger.toast(toast)
    }
}