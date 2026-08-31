package com.yifeplayte.wommo.hook.utils

import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed

/**
 * 判断当前进程是否是 [packageName] 对应的目标进程
 *
 * libxposed 下 system_server 用虚拟包名 "system" 表示，
 * 而 [EzXposed.isSystemServer] 可判断当前是否为 system_server 进程。
 * 注意 "android" 包名仍有有效 scope（其 :ui 等组件不在 system_server 中运行）。
 */
fun isHostPackage(packageName: String): Boolean =
    if (packageName == "system") EzXposed.isSystemServer else EzXposed.packageName == packageName

/**
 * 当前宿主包名；system_server 下为 "system"
 */
val hostPackageName: String
    get() = if (EzXposed.isSystemServer) "system" else EzXposed.packageName
