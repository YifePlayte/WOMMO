package com.yifeplayte.wommo.utils

import com.yifeplayte.wommo.App

/**
 * 触发所有运行中作用域进程的热重载
 *
 * 当用户在设置界面切换开关后调用此函数，
 * XposedService 会通知所有目标进程重新加载模块，
 * hook 侧的 onHotReloaded 回调会重新安装 hooks 并读取最新配置。
 */
fun reloadAllTargets() {
    val service = App.mService ?: return
    val targets = service.runningTargets ?: return
    for (target in targets) {
        service.hotReloadModule(target, null) { _, _ -> }
    }
}
