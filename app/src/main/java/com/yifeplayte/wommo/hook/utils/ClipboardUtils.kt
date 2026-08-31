package com.yifeplayte.wommo.hook.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed

object ClipboardUtils {
    @JvmStatic
    fun copy(string: String, toast: String? = null) {
        val context = EzXposed.appContext
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("WOMMO", string))
        if (toast != null) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
