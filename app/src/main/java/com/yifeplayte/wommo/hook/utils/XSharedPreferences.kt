package com.yifeplayte.wommo.hook.utils

import com.yifeplayte.wommo.BuildConfig
import de.robv.android.xposed.XSharedPreferences

/**
 * XSharedPreferences 工具
 */
@Suppress("unused")
object XSharedPreferences {
    /**
     * XSharedPreferences name
     */
    const val prefFileName = "config"

    /**
     * 获取对应的 Boolean 属性值
     * @param key 属性名称
     * @param defValue 默认值
     */
    fun getBoolean(key: String, defValue: Boolean): Boolean {
        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, prefFileName)
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getBoolean(key, defValue)
    }

    /**
     * 获取对应的 Int 属性值
     * @param key 属性名称
     * @param defValue 默认值
     */
    fun getInt(key: String, defValue: Int): Int {
        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, prefFileName)
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getInt(key, defValue)
    }

    /**
     * 获取对应的 String 属性值
     * @param key 属性名称
     * @param defValue 默认值
     */
    fun getString(key: String, defValue: String): String {
        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, prefFileName)
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getString(key, defValue) ?: defValue
    }

    /**
     * 获取对应的 StringSet 属性值
     * @param key 属性名称
     * @param defValue 默认值
     */
    fun getStringSet(key: String, defValue: MutableSet<String>): MutableSet<String> {
        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, prefFileName)
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        return prefs.getStringSet(key, defValue) ?: defValue
    }

}