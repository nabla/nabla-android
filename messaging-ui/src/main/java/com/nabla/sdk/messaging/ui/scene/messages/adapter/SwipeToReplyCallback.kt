package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.content.Context
import android.graphics.Canvas
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.math.MathUtils
import com.nabla.sdk.core.ui.helpers.DensityExtensions.dpToPx
import com.nabla.sdk.messaging.ui.R
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

internal class SwipeToReplyCallback(
    context: Context,
    private val swipeToReplyItemViewTypes: List<Int>,
    private val onSwiped: (viewHolder: RecyclerView.ViewHolder) -> Unit,
) : ItemTouchHelper.SimpleCallback(
    /*dragDirections*/ DIRECTION_DISABLED,
    /*swipeDirections*/ DIRECTION_DISABLED,
) {
    private val icon = ContextCompat.getDrawable(context, R.drawable.nabla_ic_reply)
    private val iconSize = context.dpToPx(24)
    private val iconMargin = context.dpToPx(16)
    private var swipedViewHolder: RecyclerView.ViewHolder? = null

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

    // disable default swipe to delete behavior
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 1f

    // disable default swipe to delete behavior
    override fun getSwipeEscapeVelocity(defaultValue: Float) = Float.MAX_VALUE

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        /* no-op */
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean,
    ) {
        val itemView = viewHolder.itemView
        val threshold = SWIPED_THRESHOLD_FRACTION * recyclerView.width

        when {
            isCurrentlyActive && dX.absoluteValue > threshold -> {
                if (swipedViewHolder == null) {
                    itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
                swipedViewHolder = viewHolder
            }
            isCurrentlyActive && dX.absoluteValue < threshold -> swipedViewHolder = null
            !isCurrentlyActive && viewHolder == swipedViewHolder && dX.absoluteValue <= 0 -> {
                swipedViewHolder = null

                onSwiped(viewHolder)
            }
        }

        if (dX.absoluteValue > 0) {
            val amount = (dX.absoluteValue / threshold).coerceIn(0f, 1f)
            canvas.drawIcon(amount, itemView)
        }
        super.onChildDraw(canvas, recyclerView, viewHolder, dX * SWIPED_THRESHOLD_FRACTION, dY, actionState, isCurrentlyActive)
    }

    private fun Canvas.drawIcon(animationProgress: Float, itemView: View) {
        val scaledMargin = MathUtils.lerp(0f, iconMargin.toFloat(), animationProgress).roundToInt().coerceIn(0, iconMargin)
        icon?.bounds?.apply {
            val scaledHeight = (iconSize * animationProgress).roundToInt()
            val scaledWidth = (iconSize * animationProgress).roundToInt()

            left = itemView.left + scaledMargin
            top = itemView.top + (itemView.height - scaledHeight) / 2
            right = itemView.left + scaledMargin + scaledWidth
            bottom = top + scaledHeight
        }
        icon?.draw(this)
    }

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (viewHolder.itemViewType in swipeToReplyItemViewTypes) {
            ItemTouchHelper.RIGHT
        } else {
            DIRECTION_DISABLED
        }
    }

    companion object {
        private const val SWIPED_THRESHOLD_FRACTION = 0.33f
        private const val DIRECTION_DISABLED = 0
    }
}
