package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.appcompat.view.ContextThemeWrapper
import com.nabla.sdk.core.annotation.NablaInternal

/**
 * Applies Nabla specific theme Overlay customization to [this] if not already the case.
 *
 * @param hasValidOverlaysAttr attr to check if the theme has already been customized with the overlay.
 * @param themeOverlayAttr attr to get the overlay as a theme attribute.
 * @param styleOverlay to check for theme overlay in custom view style.
 * @param defaultThemeOverlay attr to get the default overlay.
 */
@NablaInternal
public fun Context.withNablaThemeOverlay(
    @AttrRes hasValidOverlaysAttr: Int,
    @AttrRes themeOverlayAttr: Int,
    styleOverlay: StyleOverlay? = null,
    @StyleRes defaultThemeOverlay: Int,
): Context {
    val outValue = TypedValue()

    // Check if theme itself implements the wanted overlay
    val hasValidOverlays = theme.resolveAttribute(hasValidOverlaysAttr, outValue, true)
    if (hasValidOverlays) {
        return this
    }

    // Check if theme contains overlay as theme attr
    val hasOverlayAsThemeAttr = theme.resolveAttribute(themeOverlayAttr, outValue, true)
    if (hasOverlayAsThemeAttr) {
        return ContextThemeWrapper(this, outValue.resourceId)
    }

    // Check if attrs contains overlay
    if (styleOverlay != null) {
        val themeOverlayApplierInAttrs = obtainStyledAttributes(
            styleOverlay.attrs,
            styleOverlay.themeOverlayStyle
        )
        val nablaOverlayInAttrs = themeOverlayApplierInAttrs.getResourceId(0, -1)

        if (nablaOverlayInAttrs != -1) {
            return ContextThemeWrapper(this, nablaOverlayInAttrs)
        }
    }

    // Neither theme nor attrs do specify Nabla overlays. Fallback to defaults.
    return ContextThemeWrapper(
        this,
        defaultThemeOverlay
    )
}

@NablaInternal
public class StyleOverlay(
    public val attrs: AttributeSet,
    @StyleableRes public val themeOverlayStyle: IntArray,
)
