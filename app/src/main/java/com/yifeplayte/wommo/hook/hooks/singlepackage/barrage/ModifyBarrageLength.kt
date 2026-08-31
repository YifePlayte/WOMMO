package com.yifeplayte.wommo.hook.hooks.singlepackage.barrage

import android.annotation.SuppressLint
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.getStaticFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.putField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getFloat
import java.util.Random

@Suppress("unused")
object ModifyBarrageLength : BaseHook() {
    override val key = "modify_barrage_length"
    override val isEnabled get() = barrageLength != 36
    private val barrageLength by lazy { getFloat("barrage_length", 36f).toInt() }
    private const val TAG = "MiBarrage"

    @SuppressLint("DiscouragedApi")
    override fun hook() {
        val clazzUiUtils = loadClass("com.xiaomi.barrage.utils.UiUtils")
        val clazzAlphaValue = loadClass("com.xiaomi.barrage.danmu.model.AlphaValue")
        loadClass("com.xiaomi.barrage.utils.BarrageWindowUtils").findMethod {
            name("addBarrageNotification"); paramCount(2)
        }.createHook {
            replace { param ->
                val thisObject = param.thisObject
                val statusBarNotification = param.args[0] as StatusBarNotification
                val isLive = param.args[1] as Boolean

                val mBarrageView = thisObject.getFieldOrNull("mBarrageView")
                val maxLinesPair =
                    thisObject.getFieldOrNullAs<HashMap<Int, Int>>("maxLinesPair")!!
                val mPreferences = thisObject.getFieldOrNull("mPreferences")!!
                val mBarrageContext = thisObject.getFieldOrNull("mBarrageContext")!!
                val mContext = thisObject.getFieldOrNull("mContext") as Context
                val mBarrageShowManager = thisObject.getFieldOrNull("mBarrageShowManager")!!

                if (!thisObject.getFieldOrNullAs<Boolean>("hasAddView")!!) {
                    Log.d(TAG, "No barrage because view not added")
                    return@replace null
                }

                maxLinesPair[1] =
                    mPreferences.callMethod("getRowNumberIndex") as Int + 1
                var speedLevel = mPreferences.callMethod("getSpeedLevel")
                speedLevel =
                    clazzUiUtils.callStaticMethod("getSpeedLevel", speedLevel)
                mBarrageContext.callMethod("setScrollSpeedFactor", speedLevel)
                mBarrageContext.callMethod("setMaximumLines", maxLinesPair)

                val mDanmuFactory = mBarrageContext.getFieldOrNull("mDanmuFactory")!!
                val danmu = mDanmuFactory.callMethod("createDanmu", 1)

                if (danmu == null || mBarrageView == null) {
                    Log.d(TAG, "No barrage because barrage or view is null")
                    return@replace null
                }

                if (mBarrageView.callMethod("isPaused") as Boolean) {
                    mBarrageView.callMethod("resume")
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
                    content = thisObject.callMethod(
                        "cutWeChatMsg", content
                    ) as String
                }

                if ("com.whatsapp" == packageName && statusBarNotification.tag.isNullOrEmpty()) {
                    Log.d(TAG, "NULL tag, removing it")
                    return@replace null
                }

                val bubbleStylePosition =
                    mPreferences.callMethod("getBubbleStyleSelectedPosition") as Int
                val textSizeLevel =
                    mPreferences.callMethod("getTextSizeLevel") as Int
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

                var customAppTextColor = clazzUiUtils.callStaticMethod(
                    "getCustomAppTextColor",
                    mContext.resources,
                    finalBubbleStylePosition,
                    mPreferences.callMethod("getDefaultEditColorPickedPosition")
                )

                if (bubbleStylePosition == 5) {
                    val pickedPositionMap = clazzUiUtils.callStaticMethod(
                        "getPickedPosition", packageName, mPreferences
                    ) as Map<*, *>
                    thisObject.putField("pickedPositionMap", pickedPositionMap)
                    if (pickedPositionMap["key_custom_app_bubble_style_picked_position"] == 0 && textSizeLevel == 3) {
                        paddingTop = 0
                        paddingBottom = 20
                    }
                    customAppTextColor = clazzUiUtils.callStaticMethod(
                        "getCustomAppTextColor",
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

                val boundsBottomRight = clazzUiUtils.callStaticMethod(
                    "dip2px", mContext, clazzUiUtils.callStaticMethod(
                        "getFontSizeScale", textSizeLevel
                    ) as Float * 18.0f
                ) as Int
                iconDrawable.setBounds(0, 0, boundsBottomRight, boundsBottomRight)

                @Suppress("DEPRECATION")
                danmu.apply {
                    putField("appUserId", statusBarNotification.userId)
                    putField("packageName", packageName)
                    putField(
                        "text", thisObject.callMethod(
                            "createSpannable", iconDrawable, text
                        )
                    )
                    putField("paddingtop", paddingTop)
                    putField("paddingbottom", paddingBottom)
                    putField("paddingright", 50)
                    putField("paddingleft", 50)
                    putField(
                        "paintIconwidth", clazzUiUtils.callStaticMethod(
                            "dip2px", mContext, 15.0f
                        )
                    )
                    putField("priority", 0.toByte())
                    putField("isLive", isLive)
                    callMethod(
                        "setTime", mBarrageView.callMethod("getCurrentTime")
                    )
                    putField(
                        "textSize", clazzUiUtils.callStaticMethod(
                            "sp2px", mContext, clazzUiUtils.callStaticMethod(
                                "getFontSize", textSizeLevel
                            )
                        )
                    )
                    putField("textColor", customAppTextColor)
                    putField(
                        "textShadowColor", mContext.getColor(
                            mContext.resources.getIdentifier(
                                "color_border_text", "color", mContext.packageName
                            )
                        )
                    )
                    putField(
                        "barrageAlpha", mPreferences.callMethod(
                            "getBarrageViewAlpha"
                        ) as Float * clazzAlphaValue.getStaticFieldOrNullAs<Int>(
                            "MAX"
                        )!!
                    )
                    putField("bubbleStylePosition", bubbleStylePosition)
                    runCatching { putField("barrageDrawable", iconDrawable) }
                    runCatching { putField("barrageMsg", text) }
                    runCatching {
                        putField(
                            "contentIntent", statusBarNotification.notification.contentIntent
                        )
                    }
                }

                mBarrageShowManager.callMethod("addDanmu", danmu)

                return@replace null
            }
        }
    }
}
