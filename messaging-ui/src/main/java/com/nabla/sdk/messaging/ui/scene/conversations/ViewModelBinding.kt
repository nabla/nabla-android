package com.nabla.sdk.messaging.ui.scene.conversations

import android.graphics.Rect
import android.view.View
import android.widget.Toast
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.ui.helpers.CoroutineScopeExtension.launchCollect
import com.nabla.sdk.core.ui.helpers.DensityExtensions.dpToPx
import com.nabla.sdk.core.ui.helpers.RecyclerViewExtension.canScrollDown
import com.nabla.sdk.core.ui.helpers.RecyclerViewExtension.scrollToTop
import com.nabla.sdk.core.ui.model.ErrorUiModel.Companion.bind
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel.State

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
    doWithViewTreeLifecycleOwner {
        it.launchCollect(viewModel.errorAlertEventFlow) { errorAlert ->
            context?.let { context ->
                Toast.makeText(context, errorAlert.errorMessageRes, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun ConversationListView.bindViewModelState(
    viewModel: ConversationListViewModel,
    conversationAdapter: ConversationListAdapter,
) {
    doWithViewTreeLifecycleOwner {
        it.lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            loadingView.isVisible = state is State.Loading
            recyclerView.isVisible = state is State.Loaded
            errorViewContainer.isVisible = state is State.Error
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

private inline fun View.doWithViewTreeLifecycleOwner(crossinline action: (lifecycleOwner: LifecycleOwner) -> Unit) {
    // We wait for the view to be attached so that viewTreeLifecycleOwner is defined
    doOnAttach {
        findViewTreeLifecycleOwner()?.let { action(it) }
    }
}

internal class DefaultOffsetsItemDecoration(
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
