package com.nabla.sdk.messaging.ui.scene.conversations

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.messaging.core.domain.entity.ConversationId

class ConversationListAdapter(
    private val onConversationClicked: (conversationId: ConversationId) -> Unit,
) : ListAdapter<ItemUiModel, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<ItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: ItemUiModel,
            newItem: ItemUiModel,
        ) = oldItem.listId == newItem.listId

        override fun areContentsTheSame(
            oldItem: ItemUiModel,
            newItem: ItemUiModel,
        ) = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (ViewType.values()[viewType]) {
            ViewType.LOADING_MORE -> LoadingMoreViewHolder.create(parent)
            ViewType.CONVERSATION_ITEM -> ConversationViewHolder.create(parent, onConversationClicked)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ItemUiModel.Conversation -> ViewType.CONVERSATION_ITEM.ordinal
            is ItemUiModel.Loading -> ViewType.LOADING_MORE.ordinal
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ItemUiModel.Conversation -> (holder as ConversationViewHolder).bind(item)
            is ItemUiModel.Loading -> Unit /* no-op */
        }
    }

    private enum class ViewType {
        LOADING_MORE,
        CONVERSATION_ITEM,
    }
}
