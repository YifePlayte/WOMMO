package com.yifeplayte.wommo.hook.hooks.singlepackage.guardprovider

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import io.github.lingqiqi5211.ezhooktool.core.findFieldOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

/**
 * 拦截设备异常状态上报
 *
 * 上报内容是 bootloader 锁状态、SELinux 状态、已启用的无障碍服务等。这条上报由
 * 统计模块的一个任务对象执行，同一个 `run()` 里还挂着十几个与之无关的分支任务，
 * 因此不能整体拦截，只能按任务编号跳过设备状态那一个分支。
 *
 * 拦截点选在任务入口而不是它调用的统计接口：统计 SDK 会吞掉自己抛出的异常，
 * 在上游抛异常并不能阻止任务继续写上报时间戳。
 */
@Suppress("unused")
object BlockDeviceStateReport : BaseHook() {
    override val key = "block_device_state_report"

    /** 设备状态上报的任务编号，与统计模块任务分发里的分支号一致 */
    private const val TASK_DEVICE_STATE = 6

    override fun hook() {
        dexKitBridge.findMethod {
            matcher {
                name("run")
                paramCount = 0
                usingStrings = listOf("Reported device_state anomaly")
            }
        }.single().getMethodInstance().let { methodRunTask ->
            val clazzTask = methodRunTask.declaringClass
            val fieldTaskId = clazzTask.findFieldOrNull {
                type(Int::class.javaPrimitiveType!!)
            } ?: error("task id field not found in ${clazzTask.name}")
            fieldTaskId.isAccessible = true

            methodRunTask.createHook {
                before { param ->
                    val taskId = param.thisObject?.let { fieldTaskId.get(it) } as? Int
                    if (taskId == TASK_DEVICE_STATE) param.result = null
                }
            }
        }
    }
}
