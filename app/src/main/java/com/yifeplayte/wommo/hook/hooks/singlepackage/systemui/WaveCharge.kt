package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import android.animation.AnimatorSet
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.hooks.singlepackage.systemui.wave.WaveChargeView
import com.yifeplayte.wommo.hook.utils.Log
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import java.lang.reflect.Modifier

@Suppress("unused")
object WaveCharge : BaseHook() {
    override val key = "wave_charge"
    private const val stringWaveChargeViewTag = "WOMMO.wave_charge"

    override fun hook() {
        val clazzChargeContainerView =
            loadClassOrNull("com.miui.charge.container.MiuiChargeContainerView")
                ?: loadClass("com.android.keyguard.charge.container.MiuiChargeContainerView")
        val clazzIChargeView = loadClass("${clazzChargeContainerView.packageName}.IChargeView")
        check(!Modifier.isAbstract(clazzIChargeView.modifiers))
        check(ValueAnimator.AnimatorUpdateListener::class.java.isAssignableFrom(clazzIChargeView))

        val constructorChargeContainerView = clazzChargeContainerView.getDeclaredConstructor(
            Context::class.java, AttributeSet::class.java, Int::class.javaPrimitiveType
        ).apply { isAccessible = true }
        val constructorIChargeView = clazzIChargeView.getDeclaredConstructor(
            Context::class.java, AttributeSet::class.java, Int::class.javaPrimitiveType
        ).apply { isAccessible = true }
        val fieldChargeView = clazzChargeContainerView.getDeclaredField("mChargeView")
            .apply { isAccessible = true }
        val fieldContentContainer = clazzIChargeView.getDeclaredField("mContentContainer")
            .apply { isAccessible = true }
        val fieldEnterAnimatorSet = clazzIChargeView.getDeclaredField("mEnterAnimatorSet")
            .apply { isAccessible = true }
        val fieldDismissAnimatorSet = clazzIChargeView.getDeclaredField("mDismissAnimatorSet")
            .apply { isAccessible = true }
        val fieldQuartOutInterpolator = clazzIChargeView.getDeclaredField("mQuartOutInterpolator")
            .apply { isAccessible = true }
        val fieldScreenSize = clazzIChargeView.getDeclaredField("mScreenSize")
            .apply { isAccessible = true }
        val methodInitAnimator = clazzIChargeView.findMethod {
            name("initAnimator")
            paramCount(0)
        }
        val methodSetProgress = clazzIChargeView.findMethod {
            name("setProgress")
            paramCount(1)
        }
        val methodStartChildAnimation = clazzIChargeView.findMethod {
            name("startAnimationOnChildView")
            paramCount(0)
        }
        val methodStopChildAnimation = clazzIChargeView.findMethod {
            name("stopChildAnimation")
            paramCount(0)
        }
        val methodSetComponentTransparent = clazzIChargeView.findMethod {
            name("setComponentTransparent")
            paramCount(1)
        }
        val methodStartDismiss = clazzIChargeView.findMethod {
            name("startDismiss")
            paramCount(1)
        }

        // A View tag survives module class-loader changes without retaining views globally.
        fun findRenderer(carrier: Any): View? {
            val contentContainer = fieldContentContainer.get(carrier) as? ViewGroup ?: return null
            return contentContainer.getChildAt(0)?.takeIf { it.tag == stringWaveChargeViewTag }
        }

        methodInitAnimator.createHook {
            after { param ->
                val renderer = findRenderer(param.thisObject) ?: return@after
                fieldEnterAnimatorSet.set(param.thisObject, renderer.callMethod("initAnimator"))
            }
        }
        methodSetProgress.createHook {
            after { param ->
                findRenderer(param.thisObject)?.callMethod("setProgress", param.args[0])
            }
        }
        methodStartChildAnimation.createHook {
            after { param ->
                findRenderer(param.thisObject)?.callMethod("startAnimationOnChildView")
            }
        }
        methodStopChildAnimation.createHook {
            after { param ->
                findRenderer(param.thisObject)?.callMethod("stopChildAnimation")
            }
        }
        methodSetComponentTransparent.createHook {
            after { param ->
                findRenderer(param.thisObject)?.callMethod("setComponentTransparent", param.args[0])
            }
        }
        methodStartDismiss.createHook {
            after { param ->
                val renderer = findRenderer(param.thisObject) ?: return@after
                val animatorSet = fieldDismissAnimatorSet.get(param.thisObject) as? AnimatorSet ?: return@after
                if (!animatorSet.isStarted) {
                    renderer.callMethod("startDismiss", param.args[0], animatorSet)
                }
            }
        }

        constructorChargeContainerView.createHook {
            after { param ->
                val container = param.thisObject as FrameLayout
                val original = fieldChargeView.get(container) as View
                val index = container.indexOfChild(original)
                var carrier: FrameLayout? = null
                runCatching {
                    check(index >= 0)
                    val context = param.args[0] as Context
                    val replacement = constructorIChargeView.newInstance(context, null, 0) as FrameLayout
                    carrier = replacement
                    val contentContainer = fieldContentContainer.get(replacement) as ViewGroup
                    val renderer = WaveChargeView(
                        context,
                        replacement,
                        fieldQuartOutInterpolator.get(replacement) as TimeInterpolator,
                        fieldScreenSize.get(replacement) as Point,
                        { error ->
                            Log.w("WaveCharge: dithering unavailable, using the original gradient", error)
                        },
                    ).apply { tag = stringWaveChargeViewTag }
                    contentContainer.addView(
                        renderer, FrameLayout.LayoutParams(-1, -1)
                    )
                    container.addView(replacement, index, original.layoutParams)
                    fieldChargeView.set(container, replacement)
                    check(fieldChargeView.get(container) === replacement)
                    container.removeView(original)
                }.onFailure { error ->
                    carrier?.let { container.removeView(it) }
                    if (fieldChargeView.get(container) !== original) {
                        fieldChargeView.set(container, original)
                    }
                    if (original.parent == null && index >= 0) {
                        container.addView(original, index, original.layoutParams)
                    }
                    Log.e("WaveCharge: keeping the original charging animation", error)
                }
            }
        }
    }
}
