package com.yifeplayte.wommo.utils

import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullUntilSuperclass
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import java.util.Enumeration

object ClassScanner {
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> scanObjectOf(
        packageName: String, classLoader: ClassLoader = ClassScanner::class.java.classLoader!!
    ): List<T> = runCatching {
        val dexPathList = getObjectOrNullUntilSuperclass(classLoader, "pathList")
        val dexElements = dexPathList?.let { getObjectOrNullAs<Array<*>>(it, "dexElements") }

        dexElements?.flatMap { element ->
            val dexFile = element?.let { getObjectOrNull(it, "dexFile") }
            val entries =
                dexFile?.let { invokeMethodBestMatch(it, "entries") } as? Enumeration<String>

            entries?.toList()?.filter { it.startsWith(packageName) }?.mapNotNull { entry ->
                runCatching {
                    val entryClass = Class.forName(entry, true, classLoader)
                    if (entryClass.name.contains("$") || !T::class.java.isAssignableFrom(entryClass)) null
                    else entryClass.getField("INSTANCE").get(null) as T?
                }.getOrNull()
            } ?: emptyList()
        }?.distinct() ?: emptyList()
    }.getOrElse {
        Log.e("ClassScanner crashed!", it)
        emptyList()
    }
}