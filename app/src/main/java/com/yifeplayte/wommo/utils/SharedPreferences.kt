package com.yifeplayte.wommo.utils

import android.content.SharedPreferences
import androidx.core.content.edit
import com.yifeplayte.wommo.App

/**
 * SharedPreferences 工具
 *
 * API 102: 通过 XposedService.getRemotePreferences 读写模块配置。
 * mSP 为实时属性：每次访问时检查 service 是否已绑定。
 */
object SharedPreferences {
    val mSP: SharedPreferences?
        get() = runCatching {
            App.mService?.getRemotePreferences("config")
        }.getOrNull()

    /** 模块是否已激活（XposedService 已绑定） */
    val isModuleActivated: Boolean
        get() = App.mService != null

    fun SharedPreferences?.put(key: String, value: Any) {
        this?.edit {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Boolean -> putBoolean(key, value)
                is Set<*> -> putStringSet(key, value as Set<String>)
                else -> throw IllegalArgumentException("Unsupported type: ${value::class.java}")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> SharedPreferences?.get(key: String, defValue: T): T {
        if (this == null) return defValue
        runCatching {
            return when (defValue) {
                is String -> getString(key, defValue) as T
                is Int -> getInt(key, defValue) as T
                is Long -> getLong(key, defValue) as T
                is Float -> getFloat(key, defValue) as T
                is Boolean -> getBoolean(key, defValue) as T
                is Set<*> -> getStringSet(key, defValue as Set<String>) as T
                else -> defValue
            }
        }
        return defValue
    }
}
