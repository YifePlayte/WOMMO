package com.yifeplayte.wommo.utils

import android.annotation.SuppressLint
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object Clazz {
    /**
     * 使用 Unsafe 修改 static final 字段
     *
     * 若此字段被内联，修改不会生效
     *
     * https://juejin.cn/post/7624100510837882906
     */
    fun Class<*>.setStaticFinal(fieldString: String, newValue: Any?) {
        try {
            // 必须这样先 set 一下，我知道他会失败，但是失败这一次后面才能成功...
            getDeclaredField(fieldString).set(null, newValue)
        } catch (_: Throwable) {
            setFieldUsingUnsafePublic(fieldString, newValue)
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun Class<*>.setFieldUsingUnsafePublic(fieldString: String, newValue: Any?) {
        val field = getDeclaredField(fieldString)
        field.isAccessible = true
        val fieldModifiersMask = field.modifiers
        val isFinalModifierPresent = (fieldModifiersMask and Modifier.FINAL) == Modifier.FINAL

        if (isFinalModifierPresent) {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val field1 = unsafeClass.getDeclaredField("theUnsafe")
            field1.isAccessible = true
            val unsafe = field1.get(null)
            val offsetMethod = Field::class.java.getDeclaredMethod("getOffset")
            offsetMethod.isAccessible = true
            val offset = offsetMethod.invoke(field)
            val putObjectMethod = unsafeClass.getMethod(
                "putObject",
                Any::class.java,
                java.lang.Long.TYPE,
                Any::class.java
            )
            putObjectMethod.invoke(unsafe, this, offset, newValue)
        } else {
            field.set(this, newValue)
        }
    }

    fun setStaticFinalObject(obj: Class<*>, fieldString: String, newValue: Any?) = obj.setStaticFinal(fieldString, newValue)
}