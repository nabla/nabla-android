package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import kotlin.math.roundToInt

fun Context.dpToPx(value: Int): Int = resources.dpToPx(value)

fun Context.spToPx(value: Int): Int = resources.spToPx(value.toFloat())

fun Resources.dpToPx(value: Int): Int = dpToPx(value.toFloat())

fun Context.dpToPx(value: Float): Int = resources.dpToPx(value)

fun Resources.dpToPx(value: Float): Int = TypedValue
    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics)
    .roundToInt()

fun Resources.spToPx(value: Float): Int = TypedValue
    .applyDimension(TypedValue.COMPLEX_UNIT_SP, value, displayMetrics)
    .roundToInt()
