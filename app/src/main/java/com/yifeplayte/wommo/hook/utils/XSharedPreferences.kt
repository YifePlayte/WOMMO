package com.yifeplayte.wommo.hook.utils

import com.yifeplayte.wommo.BuildConfig
import de.robv.android.xposed.XSharedPreferences

object XSharedPreferences {
    const val prefFileName = "config"
    fun getBoolean(key: String, defValue: Boolean): Boolean {
        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, prefFileName)
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getBoolean(key, defValue)
    }

    fun getInt(key: String, defValue: Int): Int {
        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, prefFileName)
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getInt(key, defValue)
    }

    fun getString(key: String, defValue: String): String {
        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, prefFileName)
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getString(key, defValue) ?: defValue
    }

    fun getStringSet(key: String, defValue: MutableSet<String>): MutableSet<String> {
        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, prefFileName)
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getStringSet(key, defValue) ?: defValue
    }

}