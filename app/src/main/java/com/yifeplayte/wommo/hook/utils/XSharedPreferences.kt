package com.yifeplayte.wommo.hook.utils

import android.content.SharedPreferences
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed

/**
 * 读取模块自身 SharedPreferences 的工具
 *
 * libxposed 用 [EzXposed.base.getRemotePreferences] 替代旧 Xposed API 的 XSharedPreferences。
 * 每次读取都重新获取实例，保证能读到模块设置页最新写入的值
 * （等价旧 XSharedPreferences 每次读取前的 reload()）
 */
@Suppress("unused")
object XSharedPreferences {
    /**
     * SharedPreferences name
     */
    const val PREFERENCES_FILE_NAME = "config"
    private fun prefs(): SharedPreferences =
        EzXposed.base.getRemotePreferences(PREFERENCES_FILE_NAME)

    /**
     * 获取对应的 Boolean 属性值
     * @param key 属性名称
     * @param defValue 默认值
     */
    fun getBoolean(key: String, defValue: Boolean): Boolean = prefs().getBoolean(key, defValue)

    /**
     * 获取对应的 Int 属性值
     * @param key 属性名称
     * @param defValue 默认值
     */
    fun getInt(key: String, defValue: Int): Int = prefs().getInt(key, defValue)

    /**
     * 获取对应的 Float 属性值
     * @param key 属性名称
     * @param defValue 默认值
     */
    fun getFloat(key: String, defValue: Float): Float = prefs().getFloat(key, defValue)

    /**
     * 获取对应的 String 属性值
     * @param key 属性名称
     * @param defValue 默认值
     */
    fun getString(key: String, defValue: String): String =
        prefs().getString(key, defValue) ?: defValue

    /**
     * 获取对应的 StringSet 属性值
     * @param key 属性名称
     * @param defValue 默认值
     */
    fun getStringSet(key: String, defValue: MutableSet<String>): MutableSet<String> =
        prefs().getStringSet(key, defValue)?.toMutableSet() ?: defValue
}
