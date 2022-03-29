package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

@ColorInt
fun Context.getThemeColor(@AttrRes themeAttr: Int): Int {
    val typedValue = TypedValue()
    return if (theme.resolveAttribute(themeAttr, typedValue, true)) {
        typedValue.data
    } else {
        // failed to resolve, returning the flashiest color.
        Color.MAGENTA
    }
}

@DrawableRes
fun Context.getThemeDrawable(@AttrRes themeAttr: Int): Int? {
    val typedValue = TypedValue()
    return if (theme.resolveAttribute(themeAttr, typedValue, true)) {
        typedValue.resourceId
    } else {
        null
    }
}
