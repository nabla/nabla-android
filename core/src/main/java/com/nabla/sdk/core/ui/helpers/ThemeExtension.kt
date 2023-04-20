package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.InvalidAppThemeException

@NablaInternal
public object ThemeExtension {
    @NablaInternal
    public fun Context.getThemeColor(@AttrRes themeAttr: Int): ColorIntOrStateList {
        val typedValue = TypedValue()
        return if (theme.resolveAttribute(themeAttr, typedValue, true)) {
            // resourceId is used when the final value is ColorStateList
            if (typedValue.resourceId != 0) {
                ColorStateListWrapper(typedValue.resourceId)
            } else {
                ColorIntWrapper(typedValue.data)
            }
        } else {
            // failed to resolve, returning the flashiest color.
            ColorIntWrapper(Color.MAGENTA)
        }
    }

    @DrawableRes
    @NablaInternal
    public fun Context.getThemeDrawable(@AttrRes themeAttr: Int): Int? {
        return resolveThemeResourceId(themeAttr)
    }

    @StyleRes
    @NablaInternal
    public fun Context.getThemeStyle(@AttrRes themeAttr: Int): Int {
        return resolveThemeResourceId(themeAttr) ?: throw InvalidAppThemeException("theme style attribute not found $themeAttr")
    }

    private fun Context.resolveThemeResourceId(themeAttr: Int): Int? {
        val typedValue = TypedValue()
        return if (theme.resolveAttribute(themeAttr, typedValue, true)) {
            typedValue.resourceId
        } else {
            null
        }
    }
}
