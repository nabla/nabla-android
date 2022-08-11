package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes

internal sealed interface ColorIntOrStateList {
    fun asColorStateList(context: Context): ColorStateList
}

internal data class ColorIntWrapper(@ColorInt val value: Int) : ColorIntOrStateList {
    override fun asColorStateList(context: Context) = ColorStateList.valueOf(value)
}

internal data class ColorStateListWrapper(@ColorRes val res: Int) : ColorIntOrStateList {
    override fun asColorStateList(context: Context) = context.getColorStateList(res)
}

internal fun TextView.setTextColor(color: ColorIntOrStateList) {
    when (color) {
        is ColorIntWrapper -> setTextColor(color.value)
        is ColorStateListWrapper -> setTextColor(color.asColorStateList(context))
    }
}

internal fun View.setBackgroundColor(color: ColorIntOrStateList) {
    when (color) {
        is ColorIntWrapper -> setBackgroundColor(color.value)
        is ColorStateListWrapper -> setBackgroundResource(color.res)
    }
}
