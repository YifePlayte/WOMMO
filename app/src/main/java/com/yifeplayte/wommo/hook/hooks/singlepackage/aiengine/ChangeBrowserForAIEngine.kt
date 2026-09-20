package com.yifeplayte.wommo.hook.hooks.singlepackage.aiengine

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_RECEIVER_FOREGROUND
import android.content.Intent.FLAG_RECEIVER_NO_ABORT
import android.content.Intent.URI_INTENT_SCHEME
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getStaticFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.newInstanceAuto
import io.github.lingqiqi5211.ezhooktool.core.putStaticField
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import com.yifeplayte.wommo.hook.utils.hostPackageName
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import org.json.JSONObject

import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getInstance
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import com.yifeplayte.wommo.utils.Build.IS_SUPPORT_ISLAND
import me.zhanghai.android.appiconloader.AppIconLoader


@Suppress("unused")
@SuppressLint("DiscouragedApi")
object ChangeBrowserForAIEngine : BaseHook() {
    override val key = "change_browser_for_ai_engine"

    private const val CHANNEL_ID = "phrase_channel_id"
    private const val NAME = "智能识别通知"
    private const val NOTIFICATION_ID = 111
    private const val TRAILING_CHARS = ".,!?;:，。！？；：、）】》〉」』]}>"
    private const val STATUS_NOT_RECOGNIZED = -103
    private const val PACKAGE_NAME_XIAOMI_BROWSER = "com.android.browser"
    private const val BROWSER_ICON_RES = "app_icon_com_android_browser"
    private const val TARGET_PACKAGE_PREFIX = "wommo_target:"
    private val drawableImageActionGo by lazy {
        EzXposed.appContext.resources.getIdentifier("image_action_go", "drawable", hostPackageName)
    }
    private val drawableNotificationIconWebsite by lazy {
        EzXposed.appContext.resources.getIdentifier("notification_icon_website", "drawable", hostPackageName)
    }
    private val methodShowNotification by lazy {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("NotificationUtils.showNotification")
            }
        }.single().getMethodInstance()
    }
    private val clazzNotificationUtils by lazy {
        methodShowNotification.declaringClass
    }
    private val methodIsShowing by lazy {
        clazzNotificationUtils.findMethod {
            notAbstract(); isStatic()
            paramsAssignableFrom(Context::class.java, String::class.java)
        }
    }
    private val clazzNotificationInfo by lazy {
        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf(
                    "NotificationInfo{iconId=",
                    ", title='",
                    "', content='",
                    "', type=",
                )
            }
        }.single().getInstance()
    }
    private val methodGetDaoInfoJson by lazy {
        clazzNotificationUtils.findMethod {
            notAbstract(); isStatic()
            paramsAssignableFrom(clazzNotificationInfo, String::class.java, Context::class.java, String::class.java)
        }
    }
    private val uriRegex by lazy {
        Regex("""(?:[A-Za-z][A-Za-z0-9+.-]*://[^\s<>"'\p{IsHan}]+)|(?:(?:www\.)?[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+(?:/[^\s<>"'\p{IsHan}]*)?)""")
    }

    // The agent generation resolves the copy-direct cue from GetCopyDirectDataHandler; the legacy
    // generation has no such class at all.
    private val clazzGetCopyDirectData by lazy {
        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("GetCopyDirectDataHandler", "action_copy_direct_icon")
            }
        }.single().getInstance()
    }
    private val isAgentGeneration by lazy {
        runCatching { clazzGetCopyDirectData }.isSuccess
    }
    private val clazzClipTextCategory by lazy {
        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("GetClipTextCategoryHandler")
            }
        }.single().getInstance()
    }
    private val methodGetClipTextCategory by lazy {
        clazzClipTextCategory.findMethod {
            notAbstract(); notStatic()
            paramsAssignableFrom(Context::class.java, Bundle::class.java)
            returnType(Bundle::class.java)
        }
    }
    private val clazzWorkflowContext by lazy {
        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("workflow context key missing: ")
            }
        }.single().getInstance()
    }
    private val methodGetContextValue by lazy {
        clazzWorkflowContext.findMethod {
            notAbstract(); notStatic()
            paramCount(1)
            returnType(Any::class.java)
        }
    }
    private val clazzIconLoader by lazy {
        dexKitBridge.findClass {
            matcher {
                usingStrings = listOf("Builtin resource not found: ")
            }
        }.single().getInstance()
    }
    private val methodLoadIcon by lazy {
        clazzIconLoader.findMethod {
            notAbstract(); notStatic()
            paramsAssignableFrom(String::class.java, String::class.java, ImageView::class.java)
        }
    }
    private val browserCueKeys by lazy {
        setOf(
            "copy_text_jump_app_package",
            "copy_text_jump_app_name",
            "copy_text_jump_app_icon_res",
            "copy_text_cue_title",
        )
    }

    override fun hook() {
        if (isAgentGeneration) {
            hookAgentBrowserInstallCheck()
            hookAgentClipTextCategory()
            hookAgentCopyDirectData()
            hookAgentCueContent()
            hookAgentCueIcon()
        } else {
            hookLegacyBrowserJump()
            hookLegacyNotification()
            hookLegacyLinkMatching()
        }
    }

    // 跳转所有应用
    private fun hookLegacyBrowserJump() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf(
                    "clipboard_open",
                    "com.android.browser"
                )
            }
        }.map { it.getMethodInstance() }.createHooks {
            replace { param ->
                Intent(Intent.ACTION_VIEW, param.args[1].toString().withHttpsIfMissing().toUri()).let {
                    (param.args[0] as Context).startActivity(it)
                }
            }
        }
    }

    // 替换通知
    @SuppressLint("NotificationPermission")
    private fun hookLegacyNotification() {
        methodShowNotification.createHook {
            before { param ->
                val context = param.args[0] as Context
                val copyText = param.args[1] as String
                val type = param.args[2] as Int
                val clipPkg = param.args[5] as String
                val copyDirectId = param.args[6] as String

                if (type != 11) return@before
                val isShowing = methodIsShowing.invoke(null, context, copyText) as Boolean
                if (isShowing) return@before

                EzXposed.initAppContext(context, true)

                // 获取待启动应用信息
                val intent = Intent(Intent.ACTION_VIEW, param.args[1].toString().withHttpsIfMissing().toUri())
                val pm = context.packageManager
                val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return@before
                val appIconLoader = AppIconLoader(dp2px(context, 50f), false, context)
                val icon = appIconLoader.loadIcon(resolveInfo.activityInfo.applicationInfo)
                val label = resolveInfo.loadLabel(pm).toString()
                resolveInfo.activityInfo.packageName

                val title = context.getString(R.string.copy_direct_action_open, label)
                val text = context.getString(R.string.copy_direct_action_open_content, copyText)
                val notificationInfo =
                    clazzNotificationInfo.newInstanceAuto(drawableNotificationIconWebsite, title, text, 11)
                val notificationManager = clazzNotificationUtils.getStaticFieldOrNullAs<NotificationManager>(
                    "notificationManager"
                ) ?: return@before

                val timeout = if (IS_SUPPORT_ISLAND) 10000L else 5000L
                clazzNotificationUtils.putStaticField(
                    "pushShowTime",
                    SystemClock.elapsedRealtime()
                )
                val notificationChannel = NotificationChannel(
                    CHANNEL_ID,
                    NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.description = "识别复制内容后显示的悬浮通知"
                notificationManager.createNotificationChannel(notificationChannel)
                val pendingIntent = getPendingIntent(context, copyText, type, clipPkg, title, copyDirectId)
                val picImage = Icon.createWithBitmap(icon)
                val picGo = Icon.createWithResource(context, drawableImageActionGo)
                val bundlePics = Bundle().apply {
                    putParcelable("miui.focus.pic_image", picImage)
                    putParcelable("miui.land.pic_image", picImage)
                    putParcelable("miui.focus.pic_go", picGo)
                }
                val extras = Bundle().apply {
                    putBundle("miui.focus.pics", bundlePics)
                }
                val contentIntent: NotificationCompat.Builder =
                    NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(IconCompat.createWithBitmap(icon))
                        .setContentTitle(title)
                        .setContentText(text)
                        .setAutoCancel(true)
                        .setTimeoutAfter(timeout)
                        .setPriority(1)
                        .setContentIntent(pendingIntent)
                        .addExtras(extras)
                val actionIntent = getNotificationStartIntent(context, copyText, type, clipPkg, title, copyDirectId).toUri(URI_INTENT_SCHEME)
                val notificationBuild = contentIntent.build()
                notificationBuild.extras.apply {
                    val daoInfoJson = methodGetDaoInfoJson.invoke(null, notificationInfo, actionIntent, context, copyText) as String?
                    putString("miui.focus.param", daoInfoJson)
                    putString("copyText", copyText)
                    putParcelable("miui.appIcon", picImage)
                }
                notificationManager.notify(NOTIFICATION_ID, notificationBuild)
                param.result = null
            }
        }
    }

    // 补充匹配链接
    private fun hookLegacyLinkMatching() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("Start to getSmartPassWordCategory")
            }
        }.map { it.getMethodInstance() }.createHooks {
            after { param ->
                val bundle = param.result as? Bundle ?: return@after
                val inputTextType = bundle.getInt("inputTextType")
                if (inputTextType != 1) return@after
                val copyText = param.args[0] as? String ?: return@after
                val uri = uriRegex.find(copyText)?.value?.trimEnd { it in TRAILING_CHARS } ?: return@after
                bundle.apply {
                    putInt("inputTextType", 11)
                    putString("smartPassWordContent", uri)
                }
            }
        }
    }

    // The install check of the browser type rejects the cue once the system browser is gone; let a
    // usable default browser stand in for it.
    private fun hookAgentBrowserInstallCheck() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("isInstallForApp: ///////////////////")
            }
        }.map { it.getMethodInstance() }
            .filter {
                it.parameterTypes.contentEquals(arrayOf(Context::class.java, String::class.java)) &&
                        it.returnType == Boolean::class.javaPrimitiveType
            }
            .createHooks {
                before { param ->
                    if (param.args.getOrNull(1) != PACKAGE_NAME_XIAOMI_BROWSER) return@before
                    val context = param.args.getOrNull(0) as? Context ?: return@before
                    if (resolveTarget(context, null) != null) param.result = true
                }
            }
    }

    // The agent generation leaves the clip text type empty when nothing matched, so the legacy
    // supplement never applies; fill it in there to keep plain links reaching the browser cue.
    private fun hookAgentClipTextCategory() {        methodGetClipTextCategory.createHook {
            after { param ->
                val input = (param.args.getOrNull(1) as? Bundle)?.getString("in") ?: return@after
                val result = param.result as? Bundle ?: return@after
                val output = result.getString("target_out") ?: return@after
                if (runCatching { JSONObject(output).optInt("status") }.getOrNull() != STATUS_NOT_RECOGNIZED) return@after
                val inputJson = runCatching { JSONObject(input) }.getOrNull() ?: return@after
                val copyText = inputJson.optString("copyText")
                val uri = uriRegex.find(copyText)?.value?.trimEnd { it in TRAILING_CHARS } ?: return@after
                val recognized = JSONObject().apply {
                    put("status", 0)
                    put("tag", "SMART_PASS_WORD_XIAOMI_BROWSER")
                    put("copyText", copyText)
                    put("copySource", inputJson.optString("clipSource"))
                    put("recognitionText", uri)
                }
                result.putString("target_out", recognized.toString())
                result.putInt("target_code", 0)
                param.result = result
            }
        }
    }

    // The agent generation builds the cue intent in a new jump method; reuse the legacy jump body.
    private fun hookAgentCopyDirectData() {
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf(
                    "clipboard_open",
                    "com.android.browser"
                )
            }
        }.map { it.getMethodInstance() }
            .filter {
                it.parameterTypes.contentEquals(arrayOf(String::class.java)) &&
                        it.returnType == Intent::class.java
            }
            .createHooks {
                before { param ->
                    val uri = (param.args[0] as? String)?.withHttpsIfMissing()?.toUri() ?: return@before
                    param.result = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
    }

    // The cue content comes from a static table that always names the Xiaomi browser.
    private fun hookAgentCueContent() {
        methodGetContextValue.createHook {
            after { param ->
                val key = param.args.getOrNull(0) ?: return@after
                val keyName = runCatching { key.javaClass.getMethod("a").invoke(key) as? String }.getOrNull() ?: return@after
                if (keyName !in browserCueKeys) return@after
                if (findContextValue(param.thisObject, "copy_text_jump_app_package") != PACKAGE_NAME_XIAOMI_BROWSER) return@after
                val target = resolveCueTarget(param.thisObject) ?: return@after
                param.result = when (keyName) {
                    "copy_text_jump_app_package" -> target.packageName
                    "copy_text_jump_app_name" -> target.label
                    "copy_text_jump_app_icon_res" -> TARGET_PACKAGE_PREFIX + target.packageName
                    else -> {
                        // The host hardcodes the cue templates in Chinese; use the module's own
                        // localized strings, preferring the host context resources.
                        val context = EzXposed.appContextOrNull
                        val appValue = context?.let {
                            runCatching {
                                EzXposed.initAppContext(it, true)
                                it.getString(R.string.copy_direct_action_open_in, target.label)
                            }.getOrNull()
                        }
                        val moduleValue = runCatching {
                            EzXposed.moduleRes.getString(R.string.copy_direct_action_open_in, target.label)
                        }.getOrNull()
                        appValue ?: moduleValue ?: return@after
                    }
                }
            }
        }
    }

    // The cue icon is a builtin browser drawable; show the real target app icon instead.
    private fun hookAgentCueIcon() {
        methodLoadIcon.createHook {
            before { param ->
                val type = param.args.getOrNull(0) as? String ?: return@before
                val value = param.args.getOrNull(1) as? String ?: return@before
                when {
                    type == "builtin" && value.startsWith(TARGET_PACKAGE_PREFIX) -> {
                        param.args[0] = "application"
                        param.args[1] = value.removePrefix(TARGET_PACKAGE_PREFIX)
                    }
                    type == "builtin" && value == BROWSER_ICON_RES -> {
                        val context = EzXposed.appContextOrNull ?: return@before
                        val browser = resolveTarget(context, null) ?: return@before
                        param.args[0] = "application"
                        param.args[1] = browser.packageName
                    }
                }
            }
        }
    }

    fun String.withHttpsIfMissing(): String = if ("://" in this) this else "https://$this"

    fun dp2px(context: Context, dpValue: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.resources.displayMetrics).toInt()

    fun getPendingIntent(context: Context, copyText: String, type: Int, clipPkg: String, title: String, copyDirectId: String): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            getNotificationStartIntent(context, copyText, type, clipPkg, title, copyDirectId),
            FLAG_UPDATE_CURRENT or FLAG_MUTABLE
        )
    }

    fun getNotificationStartIntent(context: Context?, copyText: String?, type: Int, clipPkg: String?, title: String?, copyDirectId: String?): Intent {
        return Intent(context, loadClass("com.xiaomi.aicr.copydirect.IntentActivity")).apply {
            putExtra("copyText", copyText)
            putExtra("type", type)
            putExtra("clipPkg", clipPkg)
            putExtra("title", title)
            val pushShowTime = clazzNotificationUtils.getStaticFieldOrNullAs<Long>("pushShowTime")
            putExtra("pushShowTime", pushShowTime)
            putExtra("copyDirectId", copyDirectId)
            addFlags(FLAG_RECEIVER_NO_ABORT)
            addFlags(FLAG_RECEIVER_FOREGROUND)
            addFlags(FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    private class ResolvedTarget(val packageName: String, val label: String, val appInfo: ApplicationInfo)

    // The cue step resolves the jump app before the cue is built; the intent URI recorded in the
    // same workflow context tells which app the action will really reach.
    private fun resolveCueTarget(workflowContext: Any?): ResolvedTarget? {
        val context = EzXposed.appContextOrNull ?: return null
        val intentUri = findContextValue(workflowContext, "intentUri")
        val data = intentUri?.let { runCatching { Intent.parseUri(it, 0).data?.toString() }.getOrNull() }
        return resolveTarget(context, data)
    }

    private fun resolveTarget(context: Context, data: String?): ResolvedTarget? {
        if (data != null) {
            resolveActivity(context, Intent(Intent.ACTION_VIEW, data.toUri()))?.let { return it }
            if (!data.startsWith("http", true)) return null
        }
        return resolveActivity(context, Intent(Intent.ACTION_VIEW, "https://example.com".toUri()))
    }

    private fun resolveActivity(context: Context, intent: Intent): ResolvedTarget? {
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
        val appInfo = resolveInfo.activityInfo?.applicationInfo ?: return null
        val packageName = resolveInfo.activityInfo.packageName
        if (packageName == PACKAGE_NAME_XIAOMI_BROWSER || packageName == "android") return null
        return ResolvedTarget(packageName, resolveInfo.loadLabel(context.packageManager).toString(), appInfo)
    }

    private fun findContextValue(workflowContext: Any?, keyName: String): String? {
        if (workflowContext == null) return null
        val field = runCatching { workflowContext.javaClass.getDeclaredField("state") }.getOrNull() ?: return null
        field.isAccessible = true
        val map = runCatching { field.get(workflowContext) as? Map<*, *> }.getOrNull() ?: return null
        for ((key, value) in map) {
            val name = runCatching { key?.javaClass?.getMethod("a")?.invoke(key) as? String }.getOrNull()
            if (name == keyName) return value as? String
        }
        return null
    }
}
