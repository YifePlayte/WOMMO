package com.yifeplayte.wommo.hook.utils

import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.MethodData

/**
 * DexKit 工具
 */
object DexKit {
    private lateinit var hostDir: String
    private var isInitialized = false
    val dexKitBridge: DexKitBridge by lazy {
        System.loadLibrary("dexkit")
        DexKitBridge.create(hostDir).also {
            isInitialized = true
        }
    }

    /**
     * 初始化 DexKit 的 apk 完整路径
     */
    fun initDexKit(loadPackageParam: LoadPackageParam) {
        hostDir = loadPackageParam.appInfo.sourceDir
    }

    /**
     * 关闭 DexKit bridge
     */
    fun closeDexKit() {
        if (isInitialized) dexKitBridge.close()
    }

    /**
     * 使用 safeClassLoader 获得 Method
     */
    fun MethodData.getMethodInstance() = this.getMethodInstance(safeClassLoader)

    /**
     * 使用 safeClassLoader 获得 Class
     */
    fun ClassData.getInstance() = this.getInstance(safeClassLoader)
}