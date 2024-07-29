package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import java.lang.Thread.currentThread

@Suppress("unused")
object FakeNonDefaultIcon : BaseHook() {
    override val key = "fake_non_default_icon"
    private val clazzFolderPreviewIconView by lazy { loadClassOrNull("com.miui.home.launcher.folder.FolderPreviewIconView") }
    private val clazzPathDataIconUtil by lazy { loadClass("com.miui.home.launcher.PathDataIconUtil") }
    private val clazzDeviceConfig by lazy { loadClass("com.miui.home.launcher.DeviceConfig") }
    override fun hook() {
        clazzDeviceConfig.methodFinder().filterByName("isDefaultIcon").single().createHook {
            returnConstant(currentThread().stackTrace.any { it.methodName == "isBlurSupported" })
        }

        loadClass("com.miui.home.recents.views.FloatingIconView").methodFinder()
            .filterByName("updateClipPath").single().createHook {
                before { param ->
                    val thisObject = param.thisObject
                    val layoutParams = param.args[0] as FrameLayout.LayoutParams
                    val scaleFactor = param.args[1] as Float

                    val animTarget = invokeMethodBestMatch(thisObject, "getAnimTarget")
                    if (clazzFolderPreviewIconView?.isInstance(animTarget) != true) return@before
                    val iconImageView =
                        invokeMethodBestMatch(animTarget!!, "getIconImageView") as? View
                            ?: return@before
                    param.result = null

                    val mClipPath = getObjectOrNullAs<Path>(thisObject, "mClipPath")!!
                    val mForegroundClipPath =
                        getObjectOrNullAs<Path>(thisObject, "mForegroundClipPath")!!
                    val mIsAdaptiveIcon =
                        getObjectOrNullAs<Boolean>(thisObject, "mIsAdaptiveIcon")!!
                    val mScaleMatrixForClipPath =
                        getObjectOrNullAs<Matrix>(thisObject, "mScaleMatrixForClipPath")!!
                    val mTaskCornerRadius =
                        getObjectOrNullAs<Float>(thisObject, "mTaskCornerRadius")!!

                    mClipPath.reset()
                    mForegroundClipPath.reset()

                    var iconVerticalEdge = if (invokeStaticMethodBestMatch(
                            clazzDeviceConfig, "isNewIcons"
                        ) as Boolean
                    ) 0f else {
                        invokeMethodBestMatch(
                            thisObject, "getIconTransparentEdge"
                        ) as Int * scaleFactor
                    }
                    var iconHorizontalEdge = iconVerticalEdge

                    val isSupportThemeAdaptiveIcon = invokeStaticMethodBestMatch(
                        clazzPathDataIconUtil, "isSupportThemeAdaptiveIcon"
                    ) as Boolean
                    val isDefaultIcon =
                        invokeStaticMethodBestMatch(clazzDeviceConfig, "isDefaultIcon") as Boolean

                    if (isSupportThemeAdaptiveIcon && !isDefaultIcon && mIsAdaptiveIcon) {
                        val iconWidth = iconImageView.width
                        val iconHeight = iconImageView.height

                        val isIconClipPathDataARect = invokeStaticMethodBestMatch(
                            clazzPathDataIconUtil, "isIconClipPathDataARect"
                        ) as Boolean
                        val pathFromPathDataForClipIcon = invokeStaticMethodBestMatch(
                            clazzPathDataIconUtil, "getPathFromPathDataForClipIcon"
                        ) as Path?

                        if (isIconClipPathDataARect) {
                            iconHorizontalEdge = ((1.0f - invokeStaticMethodBestMatch(
                                clazzPathDataIconUtil, "getPathDataWidthPercent"
                            ) as Float) * iconWidth) / 2.0f
                            iconVerticalEdge = ((1.0f - invokeStaticMethodBestMatch(
                                clazzPathDataIconUtil, "getPathDataHeightPercent"
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

        loadClass("com.miui.home.recents.views.FloatingIconView2").methodFinder()
            .filterByName("updateClipPath").singleOrNull()?.createHook {
                before { param ->
                    val thisObject = param.thisObject
                    val layoutParams = param.args[0] as FrameLayout.LayoutParams
                    val scaleFactor = param.args[1] as Float
                    val cornerRadiusScale = param.args[2] as Float
                    val rectF = param.args[3] as RectF

                    val animTarget = invokeMethodBestMatch(param.thisObject, "getAnimTarget")
                    if (clazzFolderPreviewIconView?.isInstance(animTarget) != true) return@before
                    val iconImageView =
                        invokeMethodBestMatch(animTarget!!, "getIconImageView") as? View
                            ?: return@before
                    param.result = null

                    val mClipPath = getObjectOrNullAs<Path>(thisObject, "mClipPath")!!
                    val mForegroundClipPath =
                        getObjectOrNullAs<Path>(thisObject, "mForegroundClipPath")!!
                    val mIsAdaptiveIcon =
                        getObjectOrNullAs<Boolean>(thisObject, "mIsAdaptiveIcon")!!
                    val mScaleMatrixForClipPath =
                        getObjectOrNullAs<Matrix>(thisObject, "mScaleMatrixForClipPath")!!
                    val mTaskCornerRadius =
                        getObjectOrNullAs<Float>(thisObject, "mTaskCornerRadius")!!
                    val mUseSurfaceShade =
                        getObjectOrNullAs<Boolean>(thisObject, "mUseSurfaceShade")!!
                    val mShadeClipPath = getObjectOrNullAs<Path>(thisObject, "mShadeClipPath")!!
                    val mCurRectF = getObjectOrNullAs<RectF>(thisObject, "mCurRectF")!!
                    val mShortcutIconImageViewRect =
                        getObjectOrNullAs<RectF>(thisObject, "mShortcutIconImageViewRect")!!

                    mClipPath.reset()
                    mForegroundClipPath.reset()

                    var iconVerticalEdge = if (invokeStaticMethodBestMatch(
                            clazzDeviceConfig, "isNewIcons"
                        ) as Boolean
                    ) 0f else {
                        invokeMethodBestMatch(
                            thisObject, "getIconTransparentEdge"
                        ) as Int * scaleFactor
                    }
                    var iconHorizontalEdge = iconVerticalEdge

                    if (mUseSurfaceShade) {
                        val shadeCornerRadius =
                            (layoutParams.width * cornerRadiusScale) / rectF.width()
                        mShadeClipPath.reset()
                        mShadeClipPath.addRoundRect(
                            iconVerticalEdge,
                            iconHorizontalEdge,
                            layoutParams.width - iconVerticalEdge,
                            layoutParams.height - iconHorizontalEdge,
                            shadeCornerRadius,
                            shadeCornerRadius,
                            Path.Direction.CW
                        )
                    }

                    val isSupportThemeAdaptiveIcon = invokeStaticMethodBestMatch(
                        clazzPathDataIconUtil, "isSupportThemeAdaptiveIcon"
                    ) as Boolean
                    val isDefaultIcon =
                        invokeStaticMethodBestMatch(clazzDeviceConfig, "isDefaultIcon") as Boolean

                    if (isSupportThemeAdaptiveIcon && !isDefaultIcon && mIsAdaptiveIcon) {
                        val iconWidth = iconImageView.width
                        val iconHeight = iconImageView.height

                        val isIconClipPathDataARect = invokeStaticMethodBestMatch(
                            clazzPathDataIconUtil, "isIconClipPathDataARect"
                        ) as Boolean
                        val pathFromPathDataForClipIcon = invokeStaticMethodBestMatch(
                            clazzPathDataIconUtil, "getPathFromPathDataForClipIcon"
                        ) as Path?

                        if (isIconClipPathDataARect) {
                            iconHorizontalEdge = ((1.0f - invokeStaticMethodBestMatch(
                                clazzPathDataIconUtil, "getPathDataWidthPercent"
                            ) as Float) * iconWidth) / 2.0f
                            iconVerticalEdge = ((1.0f - invokeStaticMethodBestMatch(
                                clazzPathDataIconUtil, "getPathDataHeightPercent"
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

                    val newCornerRadius =
                        mTaskCornerRadius * mShortcutIconImageViewRect.width() / mCurRectF.width()

                    mClipPath.addRoundRect(
                        iconHorizontalEdge,
                        iconVerticalEdge,
                        layoutParams.width - iconHorizontalEdge,
                        layoutParams.height - iconVerticalEdge,
                        newCornerRadius,
                        newCornerRadius,
                        Path.Direction.CW
                    )

                    mForegroundClipPath.addRoundRect(
                        iconHorizontalEdge,
                        iconVerticalEdge,
                        layoutParams.width - iconHorizontalEdge,
                        layoutParams.width - iconVerticalEdge,
                        newCornerRadius,
                        newCornerRadius,
                        Path.Direction.CW
                    )
                }
            }
    }
}