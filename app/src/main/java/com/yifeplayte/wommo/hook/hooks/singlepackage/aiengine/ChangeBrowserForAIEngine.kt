package com.yifeplayte.wommo.hook.hooks.singlepackage.aiengine

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.EzXHelper.initAppContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.R
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_SUPPORT_ISLAND
import me.zhanghai.android.appiconloader.AppIconLoader


@Suppress("unused")
@SuppressLint("DiscouragedApi")
object ChangeBrowserForAIEngine : BaseHook() {
    override val key = "change_browser_for_ai_engine"

    const val CHANNEL_ID = "phrase_channel_id"
    const val NAME = "智能识别通知"
    const val NOTIFICATION_ID = 111
    private val drawableImageActionGo by lazy {
        appContext.resources.getIdentifier("image_action_go", "drawable", hostPackageName)
    }

    @SuppressLint("NotificationPermission")
    override fun hook() {
        // 跳转所有应用
        val clazzSmartPasswordUtils = loadClass("com.xiaomi.aicr.copydirect.util.SmartPasswordUtils")
        clazzSmartPasswordUtils.methodFinder().filterByName("jumpToXiaoMiBrowser").single().createHook {
            replace { param ->
                Intent(Intent.ACTION_VIEW, param.args[1].toString().withHttpsIfMissing().toUri()).let {
                    (param.args[0] as Context).startActivity(it)
                }
            }
        }
        // 替换通知
        val clazzNotificationUtils = loadClass("com.xiaomi.aicr.copydirect.util.NotificationUtils")
        clazzNotificationUtils.methodFinder().filterByName("showNotification").single().createHook {
            before { param ->
                val context = param.args[0] as Context
                val copyText = param.args[1] as String
                val type = param.args[2] as Int
                val clipPkg = param.args[5] as String
                val copyDirectId = param.args[6] as String

                if (type != 11) return@before
                val isInstallForApp = invokeStaticMethodBestMatch(
                    clazzSmartPasswordUtils,
                    "isInstallForApp",
                    null,
                    context, type, copyText
                ) as Boolean
                if (!isInstallForApp) return@before
                val isShowing = invokeStaticMethodBestMatch(
                    clazzNotificationUtils,
                    "isShowing",
                    null,
                    context, copyText
                ) as Boolean
                if (isShowing) return@before

                initAppContext(context, true)

                // 获取待启动应用信息
                val intent = Intent(Intent.ACTION_VIEW, param.args[1].toString().withHttpsIfMissing().toUri())
                val pm = context.packageManager
                val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return@before
                val appIconLoader = AppIconLoader(dp2px(context, 50f), false, context)
                val icon = appIconLoader.loadIcon(resolveInfo.activityInfo.applicationInfo)
                val label = resolveInfo.loadLabel(pm).toString()
                val packageName = resolveInfo.activityInfo.packageName

                val title = context.getString(R.string.copy_direct_action_open, label)
                val text = context.getString(R.string.copy_direct_action_open_content, copyText)
                val notificationInfo = invokeStaticMethodBestMatch(
                    clazzNotificationUtils,
                    "getNotificationInfo",
                    null,
                    context, type, copyText
                )?.apply {
                    setObject(this, "title", title)
                    setObject(this, "content", text)
                }

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
                val pendingIntent =
                    invokeStaticMethodBestMatch(
                        clazzNotificationUtils,
                        "getPendingIntent",
                        null,
                        context, copyText, type, clipPkg, title, copyDirectId
                    ) as PendingIntent
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
                val actionIntent =
                    invokeStaticMethodBestMatch(
                        clazzNotificationUtils,
                        "getActionIntent",
                        null,
                        context, copyText, type, clipPkg, title, copyDirectId
                    ) as String?
                val notificationBuild = contentIntent.build()
                notificationBuild.extras.apply {
                    val daoInfoJson =
                        invokeStaticMethodBestMatch(
                            clazzNotificationUtils,
                            "getDaoInfoJson",
                            null,
                            notificationInfo, actionIntent, context, copyText
                        ) as String?
                    putString("miui.focus.param", daoInfoJson)
                    putString("copyText", copyText)
                    putParcelable("miui.appIcon", picImage)
                }
                notificationManager.notify(NOTIFICATION_ID, notificationBuild)
                param.result = null
            }
        }
    }

    fun String.withHttpsIfMissing(): String = if (startsWith("http://", true) || startsWith("https://", true)) this else "https://$this"

    fun dp2px(context: Context, dpValue: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.resources.displayMetrics).toInt()
}