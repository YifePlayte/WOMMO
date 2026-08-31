package com.yifeplayte.wommo.utils

import android.util.Log
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import java.util.Enumeration

/**
 * 通过反射扫描模块自身 dex 中指定包名下的单例对象
 */
object ClassScanner {
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> scanObjectOf(
        packageName: String, classLoader: ClassLoader = ClassScanner::class.java.classLoader!!
    ): List<T> = runCatching {
        val dexPathList = classLoader.getFieldOrNull("pathList") ?: return@runCatching emptyList()
        val dexElements = dexPathList.getFieldOrNullAs<Array<*>>("dexElements") ?: return@runCatching emptyList()

        dexElements.asSequence().flatMap { element ->
            val dexFile = element?.getFieldOrNull("dexFile") ?: return@flatMap emptySequence()
            val entries = dexFile.callMethod("entries") as? Enumeration<String> ?: return@flatMap emptySequence()
            entries.asSequence().filter {
                val len = packageName.length
                return@filter it.startsWith(packageName)
                        && (it.length == len || it[len] == '.')
                        && !it.contains("$")
            }
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
        Log.e("ClassScanner", "scanObjectOf crashed", it)
        emptyList()
    }
}
