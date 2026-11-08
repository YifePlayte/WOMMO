package com.yifeplayte.wommo.hook.hooks.singlepackage.guardprovider

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import com.yifeplayte.wommo.hook.utils.XSharedPreferences
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

/**
 * 拦截云端下发的单条指令
 *
 * MiPush 透传消息里的每条 action 最终都经过同一个分发方法，指令名由参数对象的
 * 无参 getter 给出。这里只挂一次 hook，按指令名查各自的开关，命中就伪造一个
 * 成功结果，既不执行指令也不产生副作用；分发方法本身仍会正常回调服务器。
 *
 * 两个方法都不按名字定位：分发方法靠两条只属于它的字符串，取值 getter 靠
 * 「无参、返回 String、被分发方法调用」的调用关系反查。
 */
@Suppress("unused")
object BlockPushCommand : BaseHook() {
    private const val KEY_PREFIX = "block_push_command_"

    private const val CMD_COLLECT_PACKAGE = "collect_package"
    private const val CMD_DISABLE_PACKAGE = "disable_package"
    private const val CMD_ENABLE_PACKAGE = "enable_package"
    private const val CMD_DISABLE_COMPONENT = "disable_component"
    private const val CMD_ENABLE_COMPONENT = "enable_component"
    private const val CMD_CLEAR_A11Y = "clear_a11y"
    private const val CMD_SCAN_AND_PROCESS = "scan_and_process"
    private const val CMD_SET_PROP = "set_prop"
    private const val CMD_SET_SETTINGS = "set_settings"
    private const val CMD_UPDATE_VIRUS_DB = "update_virus_db"

    private val commands = listOf(
        CMD_COLLECT_PACKAGE,
        CMD_DISABLE_PACKAGE,
        CMD_ENABLE_PACKAGE,
        CMD_DISABLE_COMPONENT,
        CMD_ENABLE_COMPONENT,
        CMD_CLEAR_A11Y,
        CMD_SCAN_AND_PROCESS,
        CMD_SET_PROP,
        CMD_SET_SETTINGS,
        CMD_UPDATE_VIRUS_DB,
    )

    override val key = "block_push_command"
    override val isEnabled get() = commands.any { isCommandBlocked(it) }

    private fun isCommandBlocked(command: String) =
        XSharedPreferences.getBoolean(KEY_PREFIX + command, false)

    override fun hook() {
        val methodDispatchCommand = dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("unknown command: ", "failed to parse params: ")
            }
        }.single().getMethodInstance()

        val methodGetActionName = dexKitBridge.findMethod {
            matcher {
                paramCount = 0
                returnType = "java.lang.String"
                callerMethods {
                    add {
                        paramCount = 2
                        usingStrings = listOf("unknown command: ", "failed to parse params: ")
                    }
                }
            }
        }.single().getMethodInstance()

        // 结果类未混淆原名不可用，只能从分发方法的返回类型上取它的构造器
        val constructorResult = methodDispatchCommand.returnType.getConstructor(
            String::class.java, Int::class.javaPrimitiveType, String::class.java
        )
        methodDispatchCommand.createHook {
            before { param ->
                val command = methodGetActionName.invoke(param.args[1]) as? String ?: return@before
                if (isCommandBlocked(command)) {
                    param.result = constructorResult.newInstance(command, 0, "blocked")
                }
            }
        }
    }
}
