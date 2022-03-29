package com.nabla.sdk.messaging.ui.scene

import android.graphics.Rect
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.ui.helpers.dpToPx
import com.nabla.sdk.messaging.ui.scene.ConversationListViewModel.State
import kotlinx.coroutines.launch

fun ConversationListView.bindViewModel(viewModel: ConversationListViewModel) {
    val conversationAdapter = ConversationListAdapter(viewModel.onConversationClicked)
    recyclerView.apply {
        adapter = conversationAdapter
        addItemDecoration(OffsetsItemDecoration())
    }

    viewModel.viewModelScope.launch {
        viewModel.stateFlow.collect { state ->
            loadingView.isVisible = state is State.Loading
            recyclerView.isVisible = state is State.Loaded

            if (state is State.Loaded) {
                conversationAdapter.submitList(state.conversations)
            }
        }
    }
}

private class OffsetsItemDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapterPosition = parent.getChildAdapterPosition(view)

        val topOffset = if (adapterPosition > 0) parent.context.dpToPx(12) else 0
        val horizontalOffset = 0
        val bottomOffset = 0

        outRect.set(
            horizontalOffset,
            topOffset,
            horizontalOffset,
            bottomOffset
        )
    }
}