package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import kotlin.math.roundToInt

internal fun Context.dpToPx(value: Int): Int = resources.dpToPx(value)

internal fun Context.spToPx(value: Int): Int = resources.spToPx(value.toFloat())

internal fun Resources.dpToPx(value: Int): Int = dpToPx(value.toFloat())

internal fun Context.dpToPx(value: Float): Int = resources.dpToPx(value)

internal fun Resources.dpToPx(value: Float): Int = TypedValue
    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics)
    .roundToInt()

internal fun Resources.spToPx(value: Float): Int = TypedValue
    .applyDimension(TypedValue.COMPLEX_UNIT_SP, value, displayMetrics)
    .roundToInt()
