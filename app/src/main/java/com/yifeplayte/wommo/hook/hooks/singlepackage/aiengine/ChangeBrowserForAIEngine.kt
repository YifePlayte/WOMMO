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
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.newInstanceBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.EzXHelper.initAppContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
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
    private val drawableImageActionGo by lazy {
        appContext.resources.getIdentifier("image_action_go", "drawable", hostPackageName)
    }
    private val drawableNotificationIconWebsite by lazy {
        appContext.resources.getIdentifier("notification_icon_website", "drawable", hostPackageName)
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
        clazzNotificationUtils.methodFinder()
            .filterNonAbstract()
            .filterStatic()
            .filterByAssignableParamTypes(Context::class.java, String::class.java)
            .single()
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
        clazzNotificationUtils.methodFinder()
            .filterNonAbstract()
            .filterStatic()
            .filterByAssignableParamTypes(clazzNotificationInfo, String::class.java, Context::class.java, String::class.java)
            .single()
    }
    private val uriRegex by lazy {
        Regex("""(?:[A-Za-z][A-Za-z0-9+.-]*://[^\s<>"'\p{IsHan}]+)|(?:(?:www\.)?[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+(?:/[^\s<>"'\p{IsHan}]*)?)""")
    }

    @SuppressLint("NotificationPermission")
    override fun hook() {
        // 跳转所有应用
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
        // 替换通知
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

                initAppContext(context, true)

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
                    newInstanceBestMatch(clazzNotificationInfo, drawableNotificationIconWebsite, title, text, 11)
                val notificationManager = getStaticObjectOrNullAs<NotificationManager>(
                    clazzNotificationUtils,
                    "notificationManager"
                ) ?: return@before

                val timeout = if (IS_SUPPORT_ISLAND) 10000L else 5000L
                setStaticObject(
                    clazzNotificationUtils,
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
        // 补充匹配链接
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
            val pushShowTime = getStaticObjectOrNullAs<Long>(clazzNotificationUtils, "pushShowTime")
            putExtra("pushShowTime", pushShowTime)
            putExtra("copyDirectId", copyDirectId)
            addFlags(FLAG_RECEIVER_NO_ABORT)
            addFlags(FLAG_RECEIVER_FOREGROUND)
            addFlags(FLAG_ACTIVITY_CLEAR_TASK)
        }
    }
}
