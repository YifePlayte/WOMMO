package com.yifeplayte.wommo.hook.hooks.singlepackage.personalassistant

import android.view.View
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge
import com.yifeplayte.wommo.hook.utils.DexKit.getInstance
import com.yifeplayte.wommo.hook.utils.DexKit.getMethodInstance
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

/**
 * Stops App Vault from painting a solid background behind stacked cards while the stack is open.
 *
 * Stacking non-MIUI widgets on App Vault puts every card in one stack. The moment the stack is
 * expanded for paging, StackTransparentWidgetHelper hands each card a
 * `pa_stack_widget_card_view_background` drawable so a translucent or transparent widget does not
 * show what lies behind the stack while it scrolls, then animates that background away on
 * collapse. That background is not the widget's own, so a transparent widget turns opaque exactly
 * while it is being looked at.
 *
 * The helper class is renamed per App Vault build while its log tag is not, so the class is found
 * by that string and the entry point by shape: the only static (View, boolean) method that reads a
 * drawable is the one that installs the background. Collapse bookkeeping is left alone, so a
 * background installed before the hook took over still fades out like stock.
 */
@Suppress("unused")
object NoStackSwipeBackground : BaseHook() {
    override val key = "no_stack_swipe_background"

    // The helper's log tag is shared with the stack view, but these two message prefixes are only
    // ever built by the helper itself.
    private const val stringRemoveAllBackgrounds = "removeAllBackgrounds: cardCount="
    private const val stringClearSolidBackground = "clearSolidBackground: animate"
    private const val methodDescContextGetDrawable =
        "Landroid/content/Context;->getDrawable(I)Landroid/graphics/drawable/Drawable;"

    override fun hook() {
        val clazzHelper = dexKitBridge.findClass {
            matcher {
                usingStrings = listOf(stringRemoveAllBackgrounds, stringClearSolidBackground)
            }
        }.single().getInstance()
        val methodSetBackground = dexKitBridge.findMethod {
            matcher {
                declaredClass(clazzHelper)
                paramTypes(View::class.java, Boolean::class.javaPrimitiveType)
                returnType(Void.TYPE)
                addInvoke(methodDescContextGetDrawable)
            }
        }.single().getMethodInstance()
        // The drawable only supplies the background this hook removes, so skipping the whole method
        // leaves the card exactly like one whose background was never installed.
        methodSetBackground.createHook {
            before { param -> param.result = null }
        }
    }
}
