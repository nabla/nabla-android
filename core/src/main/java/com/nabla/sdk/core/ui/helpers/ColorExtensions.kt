package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public sealed interface ColorIntOrStateList {
    public fun asColorStateList(context: Context): ColorStateList
}

@NablaInternal
public data class ColorIntWrapper(@ColorInt val value: Int) : ColorIntOrStateList {
    override fun asColorStateList(context: Context): ColorStateList = ColorStateList.valueOf(value)
}

@NablaInternal
public data class ColorStateListWrapper(@ColorRes val res: Int) : ColorIntOrStateList {
    override fun asColorStateList(context: Context): ColorStateList = context.getColorStateList(res)
}

@NablaInternal
public fun TextView.setTextColor(color: ColorIntOrStateList) {
    when (color) {
        is ColorIntWrapper -> setTextColor(color.value)
        is ColorStateListWrapper -> setTextColor(color.asColorStateList(context))
    }
}

@NablaInternal
public fun View.setBackgroundColor(color: ColorIntOrStateList) {
    when (color) {
        is ColorIntWrapper -> setBackgroundColor(color.value)
        is ColorStateListWrapper -> setBackgroundResource(color.res)
    }
}
