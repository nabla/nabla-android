package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes

@ColorInt
internal fun Context.getThemeColor(@AttrRes themeAttr: Int): Int {
    val typedValue = TypedValue()
    return if (theme.resolveAttribute(themeAttr, typedValue, true)) {
        // resourceId is used when the final value is ColorStateList
        if (typedValue.resourceId != 0) {
            getColor(typedValue.resourceId)
        } else typedValue.data
    } else {
        // failed to resolve, returning the flashiest color.
        Color.MAGENTA
    }
}

@DrawableRes
internal fun Context.getThemeDrawable(@AttrRes themeAttr: Int): Int? {
    return resolveThemeResourceId(themeAttr)
}

@StyleRes
internal fun Context.getThemeStyle(@AttrRes themeAttr: Int): Int {
    return resolveThemeResourceId(themeAttr) ?: error("theme style attribute not found $themeAttr")
}

private fun Context.resolveThemeResourceId(themeAttr: Int): Int? {
    val typedValue = TypedValue()
    return if (theme.resolveAttribute(themeAttr, typedValue, true)) {
        typedValue.resourceId
    } else {
        null
    }
}
