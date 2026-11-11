package com.yifeplayte.wommo.hook.hooks.singlepackage.voiceassist

import com.yifeplayte.wommo.hook.hooks.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.io.File

@Suppress("unused")
object RedirectBluetoothVoiceFileDir : BaseHook() {
    override val key = "redirect_bluetooth_voice_file_dir"
    override fun hook() {
        // The Bluetooth voice SDK builds VoiceManager with
        // splicingFilePath(pkgName, "audio", null, null) unconditionally. That helper appends every
        // argument to Environment.getExternalStorageDirectory() and mkdirs each segment, so the
        // public <pkg>/audio survives even while rcsp.debug_voice_file is off. Redirect the whole
        // result into the app private dir; arg0 is only the public root's package segment, so the
        // sandbox path replaces it.
        loadClass("com.xiaomi.aivsbluetoothsdk.utils.FileUtil").findMethod {
            name("splicingFilePath")
            parameterTypes(
                String::class.java,
                String::class.java,
                String::class.java,
                String::class.java,
            )
        }.createHook {
            replace { param ->
                param.args
                    .drop(1)
                    .filterIsInstance<String>()
                    .flatMap { it.split(File.separatorChar) }
                    .filter { it.isNotEmpty() }
                    .fold(File(EzXposed.appContext.filesDir, "bluetooth_voice")) { dir, segment ->
                        File(dir, segment)
                    }
                    .also { it.mkdirs() }
                    .absolutePath
            }
        }
    }
}
