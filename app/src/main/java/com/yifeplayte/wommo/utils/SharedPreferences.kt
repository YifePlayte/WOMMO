package com.yifeplayte.wommo.utils

import android.app.Activity.MODE_WORLD_READABLE
import android.content.SharedPreferences
import androidx.core.content.edit
import com.yifeplayte.wommo.activity.MainActivity.Companion.appContext

/**
 * SharedPreferences 工具
 */
object SharedPreferences {
    @Suppress("DEPRECATION", "WorldReadableFiles")
    val mSP: SharedPreferences? by lazy {
        runCatching {
            appContext.getSharedPreferences("config", MODE_WORLD_READABLE)
        }.getOrNull()
    }

    @Suppress("UNCHECKED_CAST")
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
