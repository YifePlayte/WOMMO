package com.yifeplayte.wommo.hook.hooks.singlepackage.barrage

import android.annotation.SuppressLint
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.ObjectUtils.setObject
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getInt
import java.util.Random

@Suppress("unused")
object ModifyBarrageLength : BaseHook() {
    override val key = "modify_barrage_length"
    override val isEnabled get() = barrageLength != 36
    private val barrageLength by lazy { getInt("barrage_length", 36) }
    private const val TAG = "MiBarrage"

    @SuppressLint("DiscouragedApi")
    override fun hook() {
        val clazzUiUtils = loadClass("com.xiaomi.barrage.utils.UiUtils")
        val clazzAlphaValue = loadClass("com.xiaomi.barrage.danmu.model.AlphaValue")
        loadClass("com.xiaomi.barrage.utils.BarrageWindowUtils").methodFinder()
            .filterByName("addBarrageNotification").filterByParamCount(2).single().createHook {
                replace { param ->
                    val thisObject = param.thisObject
                    val statusBarNotification = param.args[0] as StatusBarNotification
                    val isLive = param.args[1] as Boolean

                    val mBarrageView = getObjectOrNull(thisObject, "mBarrageView")
                    val maxLinesPair =
                        getObjectOrNullAs<HashMap<Int, Int>>(thisObject, "maxLinesPair")!!
                    val mPreferences = getObjectOrNull(thisObject, "mPreferences")!!
                    val mBarrageContext = getObjectOrNull(thisObject, "mBarrageContext")!!
                    val mContext = getObjectOrNull(thisObject, "mContext") as Context
                    val mBarrageShowManager = getObjectOrNull(thisObject, "mBarrageShowManager")!!

                    if (!getObjectOrNullAs<Boolean>(thisObject, "hasAddView")!!) {
                        Log.d(TAG, "No barrage because view not added")
                        return@replace null
                    }

                    maxLinesPair[1] =
                        invokeMethodBestMatch(mPreferences, "getRowNumberIndex") as Int + 1
                    var speedLevel = invokeMethodBestMatch(mPreferences, "getSpeedLevel")
                    speedLevel =
                        invokeStaticMethodBestMatch(clazzUiUtils, "getSpeedLevel", null, speedLevel)
                    invokeMethodBestMatch(mBarrageContext, "setScrollSpeedFactor", null, speedLevel)
                    invokeMethodBestMatch(mBarrageContext, "setMaximumLines", null, maxLinesPair)

                    val mDanmuFactory = getObjectOrNull(mBarrageContext, "mDanmuFactory")!!
                    val danmu = invokeMethodBestMatch(mDanmuFactory, "createDanmu", null, 1)

                    if (danmu == null || mBarrageView == null) {
                        Log.d(TAG, "No barrage because barrage or view is null")
                        return@replace null
                    }

                    if (invokeMethodBestMatch(mBarrageView, "isPaused") as Boolean) {
                        invokeMethodBestMatch(mBarrageView, "resume")
                    }

                    val extras = statusBarNotification.notification.extras
                    val title = extras.getCharSequence("android.title").toString()
                    Log.d(TAG, "Danmu title: $title")
                    var content = extras.getCharSequence("android.text").toString()
                    Log.d(TAG, "Danmu content length: " + content.length)

                    if (title.isEmpty() || content.isEmpty()) {
                        Log.d(TAG, "No barrage because title or content is empty")
                        return@replace null
                    }

                    val packageName = statusBarNotification.packageName

                    if ("com.tencent.mm" == packageName) {
                        content = invokeMethodBestMatch(
                            thisObject, "cutWeChatMsg", null, content
                        ) as String
                    }

                    if ("com.whatsapp" == packageName && statusBarNotification.tag.isNullOrEmpty()) {
                        Log.d(TAG, "NULL tag, removing it")
                        return@replace null
                    }

                    val bubbleStylePosition =
                        invokeMethodBestMatch(mPreferences, "getBubbleStyleSelectedPosition") as Int
                    val textSizeLevel =
                        invokeMethodBestMatch(mPreferences, "getTextSizeLevel") as Int
                    var finalBubbleStylePosition = bubbleStylePosition

                    var paddingTop = 15
                    var paddingBottom = 15

                    if (bubbleStylePosition == 4 && textSizeLevel == 3) {
                        finalBubbleStylePosition = Random().nextInt(4)
                        if (finalBubbleStylePosition == 0) {
                            paddingTop = 0
                            paddingBottom = 20
                        }
                    }

                    var customAppTextColor = invokeStaticMethodBestMatch(
                        clazzUiUtils,
                        "getCustomAppTextColor",
                        null,
                        mContext.resources,
                        finalBubbleStylePosition,
                        invokeMethodBestMatch(mPreferences, "getDefaultEditColorPickedPosition")
                    )

                    if (bubbleStylePosition == 5) {
                        val pickedPositionMap = invokeStaticMethodBestMatch(
                            clazzUiUtils, "getPickedPosition", null, packageName, mPreferences
                        ) as Map<*, *>
                        setObject(thisObject, "pickedPositionMap", pickedPositionMap)
                        if (pickedPositionMap["key_custom_app_bubble_style_picked_position"] == 0 && textSizeLevel == 3) {
                            paddingTop = 0
                            paddingBottom = 20
                        }
                        customAppTextColor = invokeStaticMethodBestMatch(
                            clazzUiUtils,
                            "getCustomAppTextColor",
                            null,
                            mContext.resources,
                            pickedPositionMap["key_custom_app_bubble_style_picked_position"],
                            pickedPositionMap["key_custom_app_color_picked_position"]
                        )
                    }

                    var text = "$title: $content".replace("(\r\n|\r|\n|\n\r)".toRegex(), "")
                    if (text.length > barrageLength) {
                        text = text.substring(0, barrageLength) + "..."
                    }

                    val iconDrawable =
                        statusBarNotification.notification.smallIcon.loadDrawable(mContext)!!

                    val boundsBottomRight = invokeStaticMethodBestMatch(
                        clazzUiUtils, "dip2px", null, mContext, invokeStaticMethodBestMatch(
                            clazzUiUtils, "getFontSizeScale", null, textSizeLevel
                        ) as Float * 18.0f
                    ) as Int
                    iconDrawable.setBounds(0, 0, boundsBottomRight, boundsBottomRight)

                    @Suppress("DEPRECATION") danmu.objectHelper {
                        setObjectUntilSuperclass("appUserId", statusBarNotification.userId)
                        setObjectUntilSuperclass("packageName", packageName)
                        setObjectUntilSuperclass(
                            "text", invokeMethodBestMatch(
                                thisObject, "createSpannable", null, iconDrawable, text
                            )
                        )
                        setObjectUntilSuperclass("paddingtop", paddingTop)
                        setObjectUntilSuperclass("paddingbottom", paddingBottom)
                        setObjectUntilSuperclass("paddingright", 50)
                        setObjectUntilSuperclass("paddingleft", 50)
                        setObjectUntilSuperclass(
                            "paintIconwidth", invokeStaticMethodBestMatch(
                                clazzUiUtils, "dip2px", null, mContext, 15.0f
                            )
                        )
                        setObjectUntilSuperclass("priority", 0.toByte())
                        setObjectUntilSuperclass("isLive", isLive)
                        invokeMethodBestMatch(
                            "setTime", null, invokeMethodBestMatch(mBarrageView, "getCurrentTime")
                        )
                        setObjectUntilSuperclass(
                            "textSize", invokeStaticMethodBestMatch(
                                clazzUiUtils, "sp2px", null, mContext, invokeStaticMethodBestMatch(
                                    clazzUiUtils, "getFontSize", null, textSizeLevel
                                )
                            )
                        )
                        setObjectUntilSuperclass("textColor", customAppTextColor)
                        setObjectUntilSuperclass(
                            "textShadowColor", mContext.getColor(
                                mContext.resources.getIdentifier(
                                    "color_border_text", "color", mContext.packageName
                                )
                            )
                        )
                        setObjectUntilSuperclass(
                            "barrageAlpha", invokeMethodBestMatch(
                                mPreferences, "getBarrageViewAlpha"
                            ) as Float * getStaticObjectOrNullAs<Int>(
                                clazzAlphaValue, "MAX"
                            )!!
                        )
                        setObjectUntilSuperclass("bubbleStylePosition", bubbleStylePosition)
                        runCatching { setObjectUntilSuperclass("barrageDrawable", iconDrawable) }
                        runCatching { setObjectUntilSuperclass("barrageMsg", text) }
                        runCatching {
                            setObjectUntilSuperclass(
                                "contentIntent", statusBarNotification.notification.contentIntent
                            )
                        }
                    }

                    invokeMethodBestMatch(mBarrageShowManager, "addDanmu", null, danmu)

                    return@replace null
                }
            }
    }
}