package com.nabla.sdk.messaging.ui.scene.conversations

import android.graphics.Rect
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.ui.helpers.canScrollDown
import com.nabla.sdk.core.ui.helpers.dpToPx
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel.State
import kotlinx.coroutines.launch

fun ConversationListView.bindViewModel(
    viewModel: ConversationListViewModel,
    itemDecoration: RecyclerView.ItemDecoration? = DefaultOffsetsItemDecoration(),
) {
    val conversationAdapter = ConversationListAdapter(viewModel.onConversationClicked)
    recyclerView.apply {
        adapter = conversationAdapter
        itemDecoration?.let { addItemDecoration(it) }
        addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (!recyclerView.canScrollDown()) {
                        viewModel.onListReachedBottom()
                    }
                }
            }
        )
    }

    viewModel.viewModelScope.launch {
        viewModel.stateFlow.collect { state ->
            loadingView.isVisible = state is State.Loading
            recyclerView.isVisible = state is State.Loaded

            if (state is State.Loaded) {
                conversationAdapter.submitList(state.items)
            }
        }
    }
}

class DefaultOffsetsItemDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapterPosition = parent.getChildAdapterPosition(view)

        val topOffset = if (adapterPosition > 0) parent.context.dpToPx(12) else 0
        val horizontalOffset = parent.context.dpToPx(16)
        val bottomOffset = 0

        outRect.set(
            horizontalOffset,
            topOffset,
            horizontalOffset,
            bottomOffset
        )
    }
}
