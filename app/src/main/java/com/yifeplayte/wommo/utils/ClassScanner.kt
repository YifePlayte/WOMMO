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
        val dexPathList = getObjectOrNullUntilSuperclass(classLoader, "pathList") ?: return@runCatching emptyList()
        val dexElements = getObjectOrNullAs<Array<*>>(dexPathList, "dexElements") ?: return@runCatching emptyList()

        dexElements.asSequence().flatMap { element ->
            val dexFile = element?.let { getObjectOrNull(it, "dexFile") } ?: return@flatMap emptySequence()
            val entries = invokeMethodBestMatch(dexFile, "entries") as? Enumeration<String> ?: return@flatMap emptySequence()
            entries.asSequence().filter { it.startsWith(packageName) && !it.contains("$") }
        }.mapNotNull { entry ->
            try {
                val entryClass = Class.forName(entry, false, classLoader)
                if (!T::class.java.isAssignableFrom(entryClass)) null
                else entryClass.getField("INSTANCE").get(null) as T?
            } catch (_: Throwable) {
                null
            }
        }.toList().distinct()
    }.getOrElse {
        Log.e("ClassScanner crashed!", it)
        emptyList()
    }
}