package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import com.nabla.sdk.core.annotation.NablaInternal
import kotlin.math.roundToInt

@NablaInternal
public object DensityExtensions {
    @NablaInternal
    public fun Context.dpToPx(value: Int): Int = resources.dpToPx(value)

    @NablaInternal
    public fun Context.spToPx(value: Int): Int = resources.spToPx(value.toFloat())

    @NablaInternal
    public fun Resources.dpToPx(value: Int): Int = dpToPx(value.toFloat())

    @NablaInternal
    public fun Context.dpToPx(value: Float): Int = resources.dpToPx(value)

    @NablaInternal
    public fun Resources.dpToPx(value: Float): Int = TypedValue
        .applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics)
        .roundToInt()

    @NablaInternal
    public fun Resources.spToPx(value: Float): Int = TypedValue
        .applyDimension(TypedValue.COMPLEX_UNIT_SP, value, displayMetrics)
        .roundToInt()
}
