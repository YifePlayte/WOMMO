package com.yifeplayte.wommo.hook.utils

import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getString

/**
 * 允许额外应用使用「AI 录音纪要」的规则集。
 *
 * 规则只支持「包名 + 通话活动类名」一种写法，每行一条：
 * ```
 * com.example.app/com.example.app.ui.CallActivity   全限定
 * com.example.app/.ui.CallActivity                  相对包名
 * ```
 * 空行以及以 `#` 或 `//` 开头的行会被忽略；缺少 `/` 的行视为无效并跳过，
 * 因为门控要求 `sAvtivityList` 里存在该活动名，只给包名无法定位通话界面。
 *
 * 这份规则同时被两个宿主使用，所以解析必须共享：
 * - `com.miui.audiomonitor` 侧决定录音纪要自己的名单与原生「启用应用」列表
 * - system_server 侧决定音频框架是否把该包当作 VoIP 应用（见 AllowExtraAiSummaryApps）
 */
@Suppress("unused")
object AiSummaryRules {
    /** 规则所在的偏好键；设置页那边是手写字面量，改名要同步。 */
    private const val RULES_KEY = "ai_summary_extra_rules"

    class Rules(
        val packages: Set<String>,
        val activities: Set<String>,
        /** 因格式非法被跳过的行，仅用于给出一次性的提示日志。 */
        val invalid: List<String>,
    )

    /**
     * 解析结果按原始字符串缓存。`supportVoipMode` 在音频回调里被高频调用，不能每次都
     * 重新读偏好并解析。缓存的是「原文 + 结果」的不可变快照并整体替换，否则两个
     * 字段分开赋值会交错出「原文已是新值、结果还是旧值」的组合，之后永远命中缓存
     * 返回旧规则，且不会自行恢复。
     */
    private class Snapshot(val raw: String, val rules: Rules?)

    @Volatile
    private var snapshot: Snapshot? = null

    fun parse(): Rules? {
        val raw = getString(RULES_KEY, "").trim()
        snapshot?.let { if (it.raw == raw) return it.rules }
        val packages = LinkedHashSet<String>()
        val activities = LinkedHashSet<String>()
        val invalid = ArrayList<String>()
        raw.lineSequence().forEach { line ->
            val rule = line.trim()
            if (rule.isEmpty() || rule.startsWith("#") || rule.startsWith("//")) return@forEach
            val pkg = rule.substringBefore('/').trim()
            val activity = rule.substringAfter('/', "").trim()
            if (pkg.isEmpty() || activity.isEmpty()) {
                invalid += rule
                return@forEach
            }
            packages += pkg
            // ".ui.Call" is relative to the package, "a.b.C" is already absolute.
            activities += if (activity.startsWith(".")) pkg + activity else activity
        }
        val rules = if (packages.isEmpty()) null else Rules(packages, activities, invalid)
        snapshot = Snapshot(raw, rules)
        return rules
    }

    /** 只要包名集合，system_server 侧用不到活动名。 */
    fun packages(): Set<String> = parse()?.packages.orEmpty()
}