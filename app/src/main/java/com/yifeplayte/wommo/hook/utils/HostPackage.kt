package com.yifeplayte.wommo.hook.utils

import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed

/**
 * system_server 的哨兵包名：BasePackage("system") 的 hooks 装在 system_server 进程里。
 */
const val SYSTEM_SERVER_PACKAGE_NAME = "system"

/**
 * 判断当前进程是否是 [packageName] 对应的目标进程
 *
 * libxposed 下 system_server 用虚拟包名 [SYSTEM_SERVER_PACKAGE_NAME] 表示，
 * 而 [EzXposed.isSystemServer] 可判断当前是否为 system_server 进程。
 */
fun isHostPackage(packageName: String): Boolean =
    if (packageName == SYSTEM_SERVER_PACKAGE_NAME) EzXposed.isSystemServer
    else EzXposed.packageName == packageName

/**
 * 当前宿主包名；system_server 下为 [SYSTEM_SERVER_PACKAGE_NAME]
 */
val hostPackageName: String
    get() = if (EzXposed.isSystemServer) SYSTEM_SERVER_PACKAGE_NAME else EzXposed.packageName
