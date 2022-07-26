package com.nabla.sdk.core.ui.helpers

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ColorStateListDrawable
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.annotation.ColorInt

internal sealed interface ColorIntOrStateList {
    fun asDrawable(): Drawable
    fun asColorStateList(): ColorStateList
}

internal data class ColorIntWrapper(@ColorInt val value: Int) : ColorIntOrStateList {
    override fun asDrawable() = ColorDrawable(value)
    override fun asColorStateList() = ColorStateList.valueOf(value)
}

internal data class ColorStateListWrapper(val value: ColorStateList) : ColorIntOrStateList {
    override fun asDrawable() = ColorStateListDrawable(value)
    override fun asColorStateList() = value
}

internal fun TextView.setTextColor(color: ColorIntOrStateList) {
    when (color) {
        is ColorIntWrapper -> setTextColor(color.value)
        is ColorStateListWrapper -> setTextColor(color.value)
    }
}
