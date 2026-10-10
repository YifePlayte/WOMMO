package com.yifeplayte.wommo.hook.hooks.multipackage

import android.content.Context
import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import com.yifeplayte.wommo.hook.utils.AiSummaryRules
import com.yifeplayte.wommo.hook.utils.AiSummaryRules.Rules
import com.yifeplayte.wommo.hook.utils.Log
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getStaticFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putStaticField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

/**
 * 允许额外应用使用「AI 录音纪要」（规则语法见 [AiSummaryRules]）。
 *
 * 这个功能横跨两个宿主，所以是一个 [BaseMultiHook]：
 *
 * - `com.miui.audiomonitor`：录音纪要自己的名单与原生「启用应用」列表
 * - `system`（system_server）：音频框架的触发广播
 *
 * 缺任何一个都不通：名单放开了但广播不发，服务不会启动；广播发了但名单里没有，
 * 悬浮卡片不会显示。
 */
@Suppress("unused")
object AllowExtraAiSummaryApps : BaseMultiHook() {
    override val key = "allow_extra_ai_summary_apps"

    override val hooks = mapOf(
        "com.miui.audiomonitor" to { hookAudioMonitor() },
        "system" to { hookAudioService() },
    )

    /**
     * 原生「启用应用」界面的 CheckBoxPreference 用一个 id 作 key，不能和云控下发的
     * `voip_open_wechat` / `voip_open_QQ` 等撞车，所以额外条目统一加专属前缀。
     */
    private const val EXTRA_KEY_PREFIX = "voip_open_extra_"

    private fun currentRules(): Rules? = AiSummaryRules.parse()

    // ---------------------------------------------------------------------
    // com.miui.audiomonitor
    // ---------------------------------------------------------------------

    /**
     * 录音纪要侧要放开的判定，全部由目标应用自己持有：
     *
     * 1. 悬浮卡片，`MiuiVoipRecorderService.updateFloatViewState()`：
     *    `sVoipPkgList.contains(pkg) || pkg == "com.tencent.wework"` **且**
     *    `sAvtivityList.contains(getRunningActivityName())`。两个名单都来自
     *    `CloudControlUtil`：优先读 Settings.Global "voiprecord" 的云控 JSON，
     *    否则用按 ro.miui.region 分区的本地默认值。
     * 2. 单应用开关，`MiuiVoipRecorderService.isRecordingAllowed()`：
     *    `SharedPreferences("data").getString(pkg, "")` 必须等于 `"true"`。这个值由
     *    原生「启用应用」界面写入，所以额外包名也得出现在那个列表里。
     *
     * 另有 `AIActionAppsUtil.isAIActionSupportedApp()` 这道 AI 能力判定，它只服务 AIoT 的
     * voipRecord/start|stop|thisMomentStatus 三个 action，即小爱语音控制那条路径；
     * 录音本身和悬浮卡片都不经过它，所以这里不放开。
     */
    private fun hookAudioMonitor() {
        currentRules()?.invalid?.forEach {
            Log.w("$key: skipped invalid rule (need pkg/activity): $it")
        }
        runCatching { hookCloudControl() }
            .onFailure { Log.e("$key: hookCloudControl failed", it) }
    }

    /**
     * `CloudControlUtil.initCloudData` 是唯一一处同时重建两个门控名单和原生界面数组的
     * 地方，所以在它的 after 里注入最省事。
     */
    private fun hookCloudControl() {
        val clazz = loadClass("com.miui.audiomonitor.voiprecord.utils.CloudControlUtil")
        clazz.findMethod {
            name("initCloudData")
            paramCount(1)
            params(Context::class.java)
            notAbstract(); isStatic()
        }.createHook {
            after { param ->
                val context = param.args[0] as? Context ?: return@after
                val rules = currentRules() ?: return@after
                clazz.getStaticFieldOrNullAs<MutableList<String>>("sVoipPkgList")
                    ?.let { list -> rules.packages.forEach { if (!list.contains(it)) list.add(it) } }
                clazz.getStaticFieldOrNullAs<MutableList<String>>("sAvtivityList")
                    ?.let { list -> rules.activities.forEach { if (!list.contains(it)) list.add(it) } }
                addToAppList(context, clazz, rules)
            }
        }
    }

    /**
     * 把额外包名追加到 `VoipRecordSettingsActivity2.SettingsFragment.refreshAppList()`
     * 遍历的那四个平行数组里，让它在原生「启用应用」界面里以普通条目的样子出现。
     * 它们都是 public static 且非 final，并且每次 `initCloudData` 都会从云控 JSON
     * （或本地默认值）重建，所以在这里追加不会累积重复项。
     *
     * 复选框读写的是 `SharedPreferences("data")` 里以包名为 key 的值，和
     * `isRecordingAllowed()` 检查的完全是同一个 key；把这份数据交给原生界面持有，
     * 正是模块不会和用户的手动开关互相覆盖的原因。
     */
    private fun addToAppList(context: Context, clazz: Class<*>, rules: Rules) {
        val packages = clazz.getStaticFieldOrNullAs<Array<String>>("sPakages") ?: return
        val titles = clazz.getStaticFieldOrNullAs<Array<String>>("sTitleName") ?: return
        val titlesEn = clazz.getStaticFieldOrNullAs<Array<String>>("sTitleName_en") ?: return
        val keys = clazz.getStaticFieldOrNullAs<Array<String>>("KAY_VOIP_APPS") ?: return
        // refreshAppList 用同一下标同时读四个数组，长度必须一致，否则直接放弃
        if (packages.size != titles.size || packages.size != titlesEn.size || packages.size != keys.size) {
            Log.e("$key: stock app arrays are not parallel, skip listing")
            return
        }
        val pm = context.packageManager
        val extraPackages = ArrayList<String>()
        val extraTitles = ArrayList<String>()
        val extraKeys = ArrayList<String>()
        rules.packages.forEach { pkg ->
            if (pkg in packages || pkg in extraPackages) return@forEach
            val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@forEach
            val label = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(pkg)
            extraPackages += pkg
            extraTitles += label
            extraKeys += "$EXTRA_KEY_PREFIX$pkg"
        }
        if (extraPackages.isEmpty()) return
        clazz.putStaticField("sPakages", packages + extraPackages.toTypedArray())
        clazz.putStaticField("sTitleName", titles + extraTitles.toTypedArray())
        clazz.putStaticField("sTitleName_en", titlesEn + extraTitles.toTypedArray())
        clazz.putStaticField("KAY_VOIP_APPS", keys + extraKeys.toTypedArray())
    }

    // ---------------------------------------------------------------------
    // system（system_server）
    // ---------------------------------------------------------------------

    /**
     * 录音纪要完全靠音频框架推送的广播才会启动。
     * `android.media.AudioServiceInjector.broadcastRecorderState()`（在
     * miui-framework.jar 上，注入 system_server 的 AudioService）里：
     *
     * ```java
     * if (!supportKaraokeMode(curPkgName) && (!supportVoipMode(curPkgName) || source != 7)) return;
     * ...
     * if (supportVoipMode(curPkgName)) {
     *     intent.setAction("miui.media.AUDIO_VOIP_RECORD_STATE_CHANGED_ACTION");
     *     context.sendBroadcast(intent, RECEIVER_PERMISSION_VOIP);
     * }
     * ```
     *
     * `supportVoipMode()` 只认 `sVoipAppList` 里硬编码的八个包，名单外的应用即使在
     * 录音纪要里放行也永远收不到触发。这里只放宽这一个判定，其余条件（录音配置里的
     * 包名、`source == 7`）仍由框架自己判断，不满足时表现与原生一致。
     */
    private fun hookAudioService() {
        loadClass("android.media.AudioServiceInjector")
            .findMethod {
                name("supportVoipMode")
                paramCount(1)
                params(String::class.java)
                notAbstract(); isStatic()
            }.createHook {
                before { param ->
                    val pkg = param.args[0] as? String ?: return@before
                    if (pkg in AiSummaryRules.packages()) param.result = true
                }
            }
    }
}