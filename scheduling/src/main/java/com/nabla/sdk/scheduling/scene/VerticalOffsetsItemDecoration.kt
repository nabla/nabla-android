package com.nabla.sdk.scheduling.scene

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.ui.helpers.DensityExtensions.dpToPx

internal class VerticalOffsetsItemDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapterPosition = parent.getChildAdapterPosition(view)

        val topOffset = if (adapterPosition > 0) parent.context.dpToPx(12) else 0

        outRect.set(
            0,
            topOffset,
            0,
            0,
        )
    }
}
