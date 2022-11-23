package com.nabla.sdk.messaging.ui.scene.conversations

import android.graphics.Rect
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.ui.helpers.canScrollDown
import com.nabla.sdk.core.ui.helpers.dpToPx
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.scrollToTop
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel.State
import kotlinx.coroutines.launch

public fun ConversationListView.bindViewModel(
    viewModel: ConversationListViewModel,
    onConversationClicked: (id: ConversationId) -> Unit,
    itemDecoration: RecyclerView.ItemDecoration? = DefaultOffsetsItemDecoration(
        spacingBetweenItemsPx = context.dpToPx(12),
        firstItemTopPaddingPx = context.dpToPx(12),
        lastItemBottomPaddingPx = context.dpToPx(92),
    ),
) {
    val conversationAdapter = setupRecyclerAdapter(viewModel, onConversationClicked, itemDecoration)
    bindViewModelState(viewModel, conversationAdapter)

    bindViewModelAlerts(viewModel)
}

private fun ConversationListView.setupRecyclerAdapter(
    viewModel: ConversationListViewModel,
    onConversationClicked: (id: ConversationId) -> Unit,
    itemDecoration: RecyclerView.ItemDecoration?,
): ConversationListAdapter {
    val conversationAdapter = ConversationListAdapter(onConversationClicked)
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
    return conversationAdapter
}

private fun ConversationListView.bindViewModelAlerts(viewModel: ConversationListViewModel) {
    findViewTreeLifecycleOwner()?.launchCollect(viewModel.errorAlertEventFlow) { errorAlert ->
        context?.let { context ->
            Toast.makeText(context, errorAlert.errorMessageRes, Toast.LENGTH_SHORT).show()
        }
    }
}

private fun ConversationListView.bindViewModelState(
    viewModel: ConversationListViewModel,
    conversationAdapter: ConversationListAdapter,
) {
    viewModel.viewModelScope.launch {
        viewModel.stateFlow.collect { state ->
            loadingView.isVisible = state is State.Loading
            recyclerView.isVisible = state is State.Loaded
            errorView.root.isVisible = state is State.Error
            emptyStateView.isVisible = state is State.Empty

            when (state) {
                is State.Loaded -> {
                    // Only scroll down automatically if we're at the bottom of the chat && there are new items
                    val shouldScrollToBottomAfterSubmit = recyclerView.canScrollDown() && conversationAdapter.itemCount < state.items.size

                    conversationAdapter.submitList(state.items) {
                        if (shouldScrollToBottomAfterSubmit) recyclerView.scrollToTop()
                    }
                }
                is State.Error -> errorView.bind(state.errorUiModel, viewModel::onRetryClicked)
                is State.Loading -> Unit // no-op
                is State.Empty -> Unit // no-op
            }
        }
    }
}

public class DefaultOffsetsItemDecoration(
    private val spacingBetweenItemsPx: Int = 0,
    private val firstItemTopPaddingPx: Int = spacingBetweenItemsPx,
    private val lastItemBottomPaddingPx: Int = 0,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapterPosition = parent.getChildAdapterPosition(view)

        val topOffset = if (adapterPosition > 0) spacingBetweenItemsPx else 0
        val bottomOffset = when (adapterPosition + 1) {
            0 -> firstItemTopPaddingPx
            parent.adapter?.itemCount ?: -1 -> lastItemBottomPaddingPx
            else -> 0
        }

        outRect.set(
            0,
            topOffset,
            0,
            bottomOffset
        )
    }
}
