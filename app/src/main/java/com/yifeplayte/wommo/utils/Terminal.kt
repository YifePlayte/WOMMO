package com.yifeplayte.wommo.utils

import com.topjohnwu.superuser.Shell
import com.yifeplayte.wommo.BuildConfig

/**
 * 指令 工具
 */
object Terminal {
    init {
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(Shell.Builder.create())
    }

    /**
     * 执行单条指令
     * @param command 指令
     * @return 指令运行输出
     */
    fun exec(command: String) = Shell.cmd(command).exec()
}
