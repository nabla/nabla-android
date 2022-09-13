package com.nabla.sdk.core.ui.helpers

import android.animation.ObjectAnimator
import android.graphics.Paint
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.animation.doOnEnd
import androidx.core.text.toSpannable
import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public fun TextView.setTextOrHide(charSequence: CharSequence?, hiddenVisibility: Int = GONE) {
    text = charSequence
    visibility = if (charSequence.isNullOrEmpty()) hiddenVisibility else VISIBLE
}

@NablaInternal
public fun TextView.setTextOrHide(@StringRes res: Int?, hiddenVisibility: Int = GONE) {
    res?.let(::setText)
    visibility = if (res == null) hiddenVisibility else VISIBLE
}

@NablaInternal
public fun TextView.strikeThrough(shouldStrike: Boolean) {
    paintFlags = if (shouldStrike) {
        paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    } else {
        paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }
}

@NablaInternal
public fun TextView.animateStrikeThrough(shouldStrikeThrough: Boolean, duration: Long) {
    if (isStrikeThrough() == shouldStrikeThrough) {
        return
    }
    if (shouldStrikeThrough) {
        val span = StrikethroughSpan()
        val spannable = text.toSpannable()

        ObjectAnimator.ofInt(0, text.length).apply {
            addUpdateListener {
                val end = it.animatedValue as Int
                spannable.setSpan(
                    span,
                    0,
                    end.coerceAtMost(spannable.length),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                text = spannable
            }
            setDuration(duration)
            doOnEnd { strikeThrough(shouldStrike = true) }
        }.start()
    } else {
        strikeThrough(shouldStrike = false)
    }
}

private fun TextView.isStrikeThrough(): Boolean {
    return paintFlags and Paint.STRIKE_THRU_TEXT_FLAG == paintFlags
}
