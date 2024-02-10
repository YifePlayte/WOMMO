package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.graphics.Matrix
import android.graphics.Path
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


object FakeNonDefaultIcon : BaseHook() {
    override val key = "fake_non_default_icon"
    private val clazzFolderPreviewIconView by lazy { loadClassOrNull("com.miui.home.launcher.folder.FolderPreviewIconView") }
    private val clazzPathDataIconUtil by lazy { loadClass("com.miui.home.launcher.PathDataIconUtil") }
    override fun hook() {
        loadClass("com.miui.home.launcher.DeviceConfig").methodFinder()
            .filterByName("isDefaultIcon").first().createHook {
                before { param ->
                    param.result =
                        Thread.currentThread().stackTrace.any { it.methodName == "isBlurSupported" }
                }
            }

        loadClass("com.miui.home.recents.views.FloatingIconView").methodFinder()
            .filterByName("updateClipPath").first().createHook {
                before { param ->
                    val layoutParams = param.args[0] as FrameLayout.LayoutParams
                    val scaleFactor = param.args[1] as Float

                    val animTarget = invokeMethodBestMatch(param.thisObject, "getAnimTarget")
                    if (clazzFolderPreviewIconView?.isInstance(animTarget) != true) return@before
                    val iconImageView =
                        invokeMethodBestMatch(animTarget!!, "getIconImageView") as? View
                            ?: return@before
                    param.result = null

                    val mClipPath = getObjectOrNullAs<Path>(param.thisObject, "mClipPath")!!
                    val mForegroundClipPath =
                        getObjectOrNullAs<Path>(param.thisObject, "mForegroundClipPath")!!
                    val mIsAdaptiveIcon =
                        getObjectOrNullAs<Boolean>(param.thisObject, "mIsAdaptiveIcon")!!
                    val mScaleMatrixForClipPath =
                        getObjectOrNullAs<Matrix>(param.thisObject, "mScaleMatrixForClipPath")!!
                    val mTaskCornerRadius =
                        getObjectOrNullAs<Float>(param.thisObject, "mTaskCornerRadius")!!

                    mClipPath.reset()
                    mForegroundClipPath.reset()

                    var iconVerticalEdge = if (invokeStaticMethodBestMatch(
                            loadClass("com.miui.home.launcher.DeviceConfig"), "isNewIcons"
                        ) as Boolean
                    ) 0f else {
                        invokeMethodBestMatch(
                            param.thisObject, "getIconTransparentEdge"
                        ) as Int * scaleFactor
                    }
                    var iconHorizontalEdge = iconVerticalEdge

                    val isSupportThemeAdaptiveIcon = invokeStaticMethodBestMatch(
                        clazzPathDataIconUtil, "isSupportThemeAdaptiveIcon"
                    ) as Boolean
                    val isDefaultIcon = invokeStaticMethodBestMatch(
                        loadClass("com.miui.home.launcher.DeviceConfig"), "isDefaultIcon"
                    ) as Boolean

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
    }
}