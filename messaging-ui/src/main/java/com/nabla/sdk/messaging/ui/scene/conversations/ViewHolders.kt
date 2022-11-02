package com.nabla.sdk.messaging.ui.scene.conversations

import android.graphics.drawable.AnimatedVectorDrawable
import android.view.LayoutInflater
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.setTextOrHide
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.ui.databinding.NablaConversationItemLoadingMoreBinding
import com.nabla.sdk.messaging.ui.databinding.NablaConversationListViewItemBinding
import com.nabla.sdk.messaging.ui.helper.bindConversationAvatar

internal class LoadingMoreViewHolder(val binding: NablaConversationItemLoadingMoreBinding) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun create(parent: ViewGroup): LoadingMoreViewHolder {
            return LoadingMoreViewHolder(NablaConversationItemLoadingMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }
}

internal class ConversationViewHolder(
    private val binding: NablaConversationListViewItemBinding,
    private val onConversationClicked: (conversationId: ConversationId) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    init {
        (binding.unreadDot.drawable as? AnimatedVectorDrawable)?.start()
    }

    fun bind(uiModel: ItemUiModel.Conversation) {
        val context = binding.context

        with(binding.conversationInboxTitle) {
            text = uiModel.title
            applyConversationListTitleStyle(this, uiModel.hasUnreadMessages)
        }
        with(binding.conversationInboxSubtitle) {
            setTextOrHide(uiModel.subtitle)
            applyConversationListSubtitleStyle(this, uiModel.hasUnreadMessages)
        }

        binding.conversationAvatarView.bindConversationAvatar(uiModel.pictureUrl, uiModel.providers.firstOrNull(), displayAvatar = true)

        binding.unreadDot.visibility = if (uiModel.hasUnreadMessages) VISIBLE else INVISIBLE
        binding.lastMessageDate.text = uiModel.formatLastModified(context)
        applyLastMessageTimeStyle(binding.lastMessageDate, uiModel.hasUnreadMessages)

        binding.root.setOnClickListener { onConversationClicked(uiModel.id) }
    }

    companion object {
        fun create(parent: ViewGroup, onConversationClicked: (conversationId: ConversationId) -> Unit): ConversationViewHolder {
            val binding = NablaConversationListViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ConversationViewHolder(binding, onConversationClicked)
        }
    }
}
