package com.yifeplayte.wommo.hook.utils

import java.lang.ref.WeakReference

/**
 * libxposed 没有 XposedHelpers.get/setAdditionalInstanceField 的等价物，
 * 这里用基于对象 identity 的弱引用表实现同等功能，避免强引用导致内存泄漏
 */
object AdditionalFields {
    private class Key(obj: Any) {
        private val ref = WeakReference(obj)
        private val hash = System.identityHashCode(obj)
        fun get(): Any? = ref.get()
        override fun hashCode(): Int = hash
        override fun equals(other: Any?): Boolean =
            other is Key && other.get() === ref.get()
    }

    private val fields = HashMap<Key, MutableMap<String, Any?>>()
    private var setCount = 0

    @Synchronized
    fun get(obj: Any, key: String): Any? = fields[Key(obj)]?.get(key)

    @Synchronized
    fun set(obj: Any, key: String, value: Any?) {
        fields.getOrPut(Key(obj)) { mutableMapOf() }[key] = value
        if (++setCount % 100 == 0) purge()
    }

    @Synchronized
    private fun purge() {
        fields.keys.removeIf { it.get() == null }
    }
}
