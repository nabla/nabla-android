package com.nabla.sdk.messaging.ui.fullscreenmedia.helper

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.res.use
import com.nabla.sdk.messaging.ui.R

/**
 * Applies Nabla specific theme Overlay customization to [this] if not already the case.
 *
 * @param attrs to check for `nablaMessagingThemeOverlay`, practical when creating a custom view.
 */
internal fun Context.withNablaMessagingThemeOverlays(attrs: AttributeSet? = null): Context {
    val outValue = TypedValue()
    val hasValidOverlays = theme.resolveAttribute(R.attr.hasValidMessagingOverlays, outValue, true)

    return if (hasValidOverlays) this else {
        val nablaOverlayInAttrs = obtainStyledAttributes(attrs, R.styleable.NablaThemeOverlayApplier).use {
            it.getResourceId(
                R.styleable.NablaThemeOverlayApplier_nablaMessagingThemeOverlay,
                -1,
            )
        }

        if (nablaOverlayInAttrs != -1) ContextThemeWrapper(this, nablaOverlayInAttrs) else {
            val hasOverlayAsThemeAttr = theme.resolveAttribute(R.attr.nablaMessagingThemeOverlay, outValue, true)

            ContextThemeWrapper(
                this,
                if (hasOverlayAsThemeAttr) outValue.resourceId else {
                    // Neither theme nor attrs do specify Nabla overlays. Fallback to defaults.
                    R.style.ThemeOverlay_Nabla_Messaging
                }
            )
        }
    }
}
