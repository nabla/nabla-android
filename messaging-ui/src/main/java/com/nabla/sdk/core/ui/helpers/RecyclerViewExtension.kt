package com.nabla.sdk.core.ui.helpers

import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.scrollToTop() {
    if ((adapter?.itemCount ?: 0) > 0) {
        scrollToPosition(0)
    }
}

private const val SCROLL_DIRECTION_TOP = -1
private const val SCROLL_DIRECTION_BOTTOM = 1

fun RecyclerView.canScrollUp(): Boolean = canScrollVertically(SCROLL_DIRECTION_TOP)
fun RecyclerView.canScrollDown(): Boolean = canScrollVertically(SCROLL_DIRECTION_BOTTOM)

fun RecyclerView.smoothSnapToPosition(position: Int, snapMode: Int = LinearSmoothScroller.SNAP_TO_START) {
    val smoothScroller = object : LinearSmoothScroller(this.context) {
        override fun getVerticalSnapPreference(): Int = snapMode
        override fun getHorizontalSnapPreference(): Int = snapMode
    }
    val coercedPosition = position.coerceAtMost(this.adapter?.itemCount?.minus(1) ?: 0).coerceAtLeast(0)
    smoothScroller.targetPosition = coercedPosition
    layoutManager?.startSmoothScroll(smoothScroller)
}

fun RecyclerView.maintainBottomPositionOnLayoutShrink() {
    this.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
        if (oldBottom > bottom) {
            this.scrollBy(0, oldBottom - bottom)
        }
    }
}
