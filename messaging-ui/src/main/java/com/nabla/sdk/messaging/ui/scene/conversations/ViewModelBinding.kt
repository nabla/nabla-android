package com.nabla.sdk.messaging.ui.scene.conversations

import android.content.Context
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
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel.ErrorAlert
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel.State
import kotlinx.coroutines.launch

fun ConversationListView.bindViewModel(
    viewModel: ConversationListViewModel,
    onConversationClicked: (id: ConversationId) -> Unit,
    itemDecoration: RecyclerView.ItemDecoration? = DefaultOffsetsItemDecoration(),
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
            Toast.makeText(context, errorAlert.defaultMessage(context), Toast.LENGTH_SHORT).show()
        }
    }
}

private fun ErrorAlert.defaultMessage(context: Context): String {
    val resId = when (this) {
        is ErrorAlert.LoadingMoreConversations -> R.string.nabla_error_message_conversations_loading_more
    }

    return context.getString(resId)
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

            when (state) {
                is State.Loaded -> conversationAdapter.submitList(state.items)
                is State.Error -> errorView.bind(state.errorUiModel, viewModel::onRetryClicked)
                is State.Loading -> Unit // no-op
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
