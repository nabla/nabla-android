package com.nabla.sdk.messaging.ui.scene

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.setTextOrHide
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.ui.databinding.ConversationListViewItemBinding

class ConversationListAdapter(
    private val onConversationClicked: (conversationId: ConversationId) -> Unit,
) : ListAdapter<ConversationItemUiModel, ConversationListAdapter.ConversationViewHolder>(
    object : DiffUtil.ItemCallback<ConversationItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: ConversationItemUiModel,
            newItem: ConversationItemUiModel,
        ) = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ConversationItemUiModel,
            newItem: ConversationItemUiModel,
        ) = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        return ConversationViewHolder.create(parent, onConversationClicked)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ConversationViewHolder(
        private val binding: ConversationListViewItemBinding,
        private val onConversationClicked: (conversationId: ConversationId) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uiModel: ConversationItemUiModel) {
            val context = binding.context

            with(binding.conversationInboxTitle) {
                text = uiModel.title
                applyConversationListTitleStyle(this, uiModel.hasUnreadMessages)
            }
            with(binding.conversationInboxSubtitle) {
                setTextOrHide(uiModel.subtitle)
                applyConversationListSubtitleStyle(this, uiModel.hasUnreadMessages)
            }

            val firstProvider = uiModel.providers.firstOrNull()
            if (firstProvider != null) {
                binding.avatarView.loadAvatar(firstProvider)
            } else {
                binding.avatarView.displaySystemAvatar()
            }

            binding.unreadDot.isVisible = uiModel.hasUnreadMessages
            binding.lastMessageDate.text = uiModel.formatLastModified(context)
            applyLastMessageTimeStyle(binding.lastMessageDate, uiModel.hasUnreadMessages)

            binding.root.setOnClickListener { onConversationClicked(uiModel.id) }
        }

        companion object {
            fun create(parent: ViewGroup, onConversationClicked: (conversationId: ConversationId) -> Unit): ConversationViewHolder {
                val binding = ConversationListViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ConversationViewHolder(binding, onConversationClicked)
            }
        }
    }
}
