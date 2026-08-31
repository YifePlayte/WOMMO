package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethodOrNull
import io.github.lingqiqi5211.ezhooktool.core.getFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.loadClassFirst
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

import com.yifeplayte.wommo.hook.hooks.BaseHook
import java.lang.Thread.currentThread

@Suppress("unused")
object FakeNonDefaultIcon : BaseHook() {
    override val key = "fake_non_default_icon"
    private val clazzFolderPreviewIconView by lazy { loadClassOrNull("com.miui.home.launcher.folder.FolderPreviewIconView") }
    private val clazzPathDataIconUtil by lazy { loadClass("com.miui.home.launcher.PathDataIconUtil") }
    private val clazzDeviceConfig by lazy {
        loadClassFirst(
            "com.miui.home.common.device.DeviceConfigs",
            "com.miui.home.launcher.DeviceConfig"
        )
    }

    override fun hook() {
        val methodIsDefaultIcon = clazzDeviceConfig.findMethod {
            filter { name in setOf("isDefaultIcon", "isDefaultMiuiIcon") }
        }
        methodIsDefaultIcon.createHook {
            returnConstant(currentThread().stackTrace.any { it.methodName == "isBlurSupported" })
        }

        loadClass("com.miui.home.recents.views.FloatingIconView").findMethod {
            name("updateClipPath")
        }.createHook {
            before { param ->
                val thisObject = param.thisObject
                val layoutParams = param.args[0] as FrameLayout.LayoutParams
                val scaleFactor = param.args[1] as Float

                val animTarget = thisObject.callMethod("getAnimTarget")
                if (clazzFolderPreviewIconView?.isInstance(animTarget) != true) return@before
                val iconImageView =
                    animTarget!!.callMethod("getIconImageView") as? View
                        ?: return@before
                param.result = null

                val mClipPath = thisObject.getFieldOrNullAs<Path>("mClipPath")!!
                val mForegroundClipPath =
                    thisObject.getFieldOrNullAs<Path>("mForegroundClipPath")!!
                val mIsAdaptiveIcon =
                    thisObject.getFieldOrNullAs<Boolean>("mIsAdaptiveIcon")!!
                val mScaleMatrixForClipPath =
                    thisObject.getFieldOrNullAs<Matrix>("mScaleMatrixForClipPath")!!
                val mTaskCornerRadius =
                    thisObject.getFieldOrNullAs<Float>("mTaskCornerRadius")!!

                mClipPath.reset()
                mForegroundClipPath.reset()

                var iconVerticalEdge = if (clazzDeviceConfig.callStaticMethod(
                        "isNewIcons"
                    ) as Boolean
                ) 0f else {
                    thisObject.callMethod(
                        "getIconTransparentEdge"
                    ) as Int * scaleFactor
                }
                var iconHorizontalEdge = iconVerticalEdge

                val isSupportThemeAdaptiveIcon = clazzPathDataIconUtil.callStaticMethod(
                    "isSupportThemeAdaptiveIcon"
                ) as Boolean
                val isDefaultIcon = methodIsDefaultIcon.invoke(null) as Boolean

                if (isSupportThemeAdaptiveIcon && !isDefaultIcon && mIsAdaptiveIcon) {
                    val iconWidth = iconImageView.width
                    val iconHeight = iconImageView.height

                    val isIconClipPathDataARect = clazzPathDataIconUtil.callStaticMethod(
                        "isIconClipPathDataARect"
                    ) as Boolean
                    val pathFromPathDataForClipIcon = clazzPathDataIconUtil.callStaticMethod(
                        "getPathFromPathDataForClipIcon"
                    ) as Path?

                    if (isIconClipPathDataARect) {
                        iconHorizontalEdge = ((1.0f - clazzPathDataIconUtil.callStaticMethod(
                            "getPathDataWidthPercent"
                        ) as Float) * iconWidth) / 2.0f
                        iconVerticalEdge = ((1.0f - clazzPathDataIconUtil.callStaticMethod(
                            "getPathDataHeightPercent"
                        ) as Float) * iconHeight) / 2.0f
                    } else if (pathFromPathDataForClipIcon != null) {
                        mClipPath.set(pathFromPathDataForClipIcon)
                        val aspectRatio = (layoutParams.height / layoutParams.width).toFloat()
                        mScaleMatrixForClipPath.reset()
                        mScaleMatrixForClipPath.setScale(
                            iconWidth / 100.0f, (iconHeight / 100.0f) * aspectRatio
                        )
                        mClipPath.transform(mScaleMatrixForClipPath)
                        return@before
                    }
                }

                mClipPath.addRoundRect(
                    iconHorizontalEdge,
                    iconVerticalEdge,
                    layoutParams.width - iconHorizontalEdge,
                    layoutParams.height - iconVerticalEdge,
                    mTaskCornerRadius,
                    mTaskCornerRadius,
                    Path.Direction.CW
                )

                mForegroundClipPath.addRoundRect(
                    iconHorizontalEdge,
                    iconVerticalEdge,
                    layoutParams.width - iconHorizontalEdge,
                    layoutParams.width - iconVerticalEdge,
                    mTaskCornerRadius,
                    mTaskCornerRadius,
                    Path.Direction.CW
                )
            }
        }

        loadClass("com.miui.home.recents.views.FloatingIconView2").findMethodOrNull {
            name("updateClipPath")
        }?.createHook {
            before { param ->
                val thisObject = param.thisObject
                val layoutParams = param.args[0]!!
                val width = layoutParams.getFieldOrNullAs<Float>("width") ?: return@before
                val height = layoutParams.getFieldOrNullAs<Float>("height") ?: return@before
                val scaleFactor = param.args[1] as Float
                val cornerRadiusScale = param.args[2] as Float
                val rectF = param.args[3] as RectF

                val animTarget = param.thisObject.callMethod("getAnimTarget")
                if (clazzFolderPreviewIconView?.isInstance(animTarget) != true) return@before
                val iconImageView =
                    animTarget!!.callMethod("getIconImageView") as? View
                        ?: return@before
                param.result = null

                val mClipPath = thisObject.getFieldOrNullAs<Path>("mClipPath")!!
                val mForegroundClipPath =
                    thisObject.getFieldOrNullAs<Path>("mForegroundClipPath")!!
                val mIsAdaptiveIcon =
                    thisObject.getFieldOrNullAs<Boolean>("mIsAdaptiveIcon")!!
                val mScaleMatrixForClipPath =
                    thisObject.getFieldOrNullAs<Matrix>("mScaleMatrixForClipPath")!!
                val mTaskCornerRadius =
                    thisObject.getFieldOrNullAs<Float>("mTaskCornerRadius")!!
                val mUseSurfaceShade =
                    thisObject.getFieldOrNullAs<Boolean>("mUseSurfaceShade")!!
                val mShadeClipPath = thisObject.getFieldOrNullAs<Path>("mShadeClipPath")!!
                val mCurRectF = thisObject.getFieldOrNullAs<RectF>("mCurRectF")!!
                val mShortcutIconImageViewRect =
                    thisObject.getFieldOrNullAs<RectF>("mShortcutIconImageViewRect")!!

                mClipPath.reset()
                mForegroundClipPath.reset()

                var iconVerticalEdge = if (clazzDeviceConfig.callStaticMethod(
                        "isNewIcons"
                    ) as Boolean
                ) 0f else {
                    thisObject.callMethod(
                        "getIconTransparentEdge"
                    ) as Int * scaleFactor
                }
                var iconHorizontalEdge = iconVerticalEdge

                if (mUseSurfaceShade) {
                    val shadeCornerRadius =
                        (width * cornerRadiusScale) / rectF.width()
                    mShadeClipPath.reset()
                    mShadeClipPath.addRoundRect(
                        iconVerticalEdge,
                        iconHorizontalEdge,
                        width - iconVerticalEdge,
                        height - iconHorizontalEdge,
                        shadeCornerRadius,
                        shadeCornerRadius,
                        Path.Direction.CW
                    )
                }

                val isSupportThemeAdaptiveIcon = clazzPathDataIconUtil.callStaticMethod(
                    "isSupportThemeAdaptiveIcon"
                ) as Boolean
                val isDefaultIcon = false

                if (isSupportThemeAdaptiveIcon && !isDefaultIcon && mIsAdaptiveIcon) {
                    val iconWidth = iconImageView.width
                    val iconHeight = iconImageView.height

                    val isIconClipPathDataARect = clazzPathDataIconUtil.callStaticMethod(
                        "isIconClipPathDataARect"
                    ) as Boolean
                    val pathFromPathDataForClipIcon = clazzPathDataIconUtil.callStaticMethod(
                        "getPathFromPathDataForClipIcon"
                    ) as Path?

                    if (isIconClipPathDataARect) {
                        iconHorizontalEdge = ((1.0f - clazzPathDataIconUtil.callStaticMethod(
                            "getPathDataWidthPercent"
                        ) as Float) * iconWidth) / 2.0f
                        iconVerticalEdge = ((1.0f - clazzPathDataIconUtil.callStaticMethod(
                            "getPathDataHeightPercent"
                        ) as Float) * iconHeight) / 2.0f
                    } else if (pathFromPathDataForClipIcon != null) {
                        mClipPath.set(pathFromPathDataForClipIcon)
                        val aspectRatio = (height / width)
                        mScaleMatrixForClipPath.reset()
                        mScaleMatrixForClipPath.setScale(
                            iconWidth / 100.0f, (iconHeight / 100.0f) * aspectRatio
                        )
                        mClipPath.transform(mScaleMatrixForClipPath)
                        return@before
                    }
                }

                val newCornerRadius =
                    mTaskCornerRadius * mShortcutIconImageViewRect.width() / mCurRectF.width()

                mClipPath.addRoundRect(
                    iconHorizontalEdge,
                    iconVerticalEdge,
                    width - iconHorizontalEdge,
                    height - iconVerticalEdge,
                    newCornerRadius,
                    newCornerRadius,
                    Path.Direction.CW
                )

                mForegroundClipPath.addRoundRect(
                    iconHorizontalEdge,
                    iconVerticalEdge,
                    width - iconHorizontalEdge,
                    width - iconVerticalEdge,
                    newCornerRadius,
                    newCornerRadius,
                    Path.Direction.CW
                )
            }
        }
    }
}
