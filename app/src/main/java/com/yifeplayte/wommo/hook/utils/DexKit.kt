package com.yifeplayte.wommo.hook.utils

import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.MethodData

/**
 * DexKit 工具
 *
 * bridge 按需创建并保留到进程结束：
 * 热重载时新 generation 的 hostDir 为空，会回退到 [EzXposed.appContextOrNull] 重新解析 apk 路径
 */
object DexKit {
    private var hostDir: String? = null
    private var bridge: DexKitBridge? = null
    private var isInitialized = false
    val dexKitBridge: DexKitBridge
        get() {
            bridge?.let { return it }
            val dir = hostDir
                ?: EzXposed.appContextOrNull?.applicationInfo?.sourceDir
                ?: error("DexKit hostDir is not initialized")
            System.loadLibrary("dexkit")
            return DexKitBridge.create(dir).also {
                bridge = it
                isInitialized = true
            }
        }

    /**
     * 初始化 DexKit 的 apk 完整路径
     */
    fun initDexKit(sourceDir: String) {
        hostDir = sourceDir
    }

    /**
     * 关闭 DexKit bridge
     */
    fun closeDexKit() {
        if (isInitialized) {
            bridge?.close()
            bridge = null
            isInitialized = false
        }
    }

    /**
     * 使用 safeClassLoader 获得 Method
     */
    fun MethodData.getMethodInstance() = this.getMethodInstance(EzXposed.safeClassLoader)

    /**
     * 使用 safeClassLoader 获得 Class
     */
    fun ClassData.getInstance() = this.getInstance(EzXposed.safeClassLoader)
}
