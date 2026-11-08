package com.yifeplayte.wommo.hook.hooks.singlepackage.guardprovider

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

/**
 * 拦截非系统应用清单上报
 *
 * 三条入口（扫描结束、AIDL detectAppsScan、引擎更新）都汇到同一个 POST
 * `https://flash.sec.miui.com/detect/app` 的发包方法，返回 null 即上传失败。
 */
@Suppress("unused")
object BlockAppListUpload : BaseHook() {
    override val key = "block_app_list_upload"
    override fun hook() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("https://flash.sec.miui.com/detect/app")
            }
        }.single().getMethodInstance().createHook {
            before { param -> param.result = null }
        }
    }
}
