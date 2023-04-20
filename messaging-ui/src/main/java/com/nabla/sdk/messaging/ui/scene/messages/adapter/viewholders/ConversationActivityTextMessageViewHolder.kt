package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.core.domain.entity.DeletedProvider
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.ui.helpers.MessageAuthorExtensions.fullNameWithPrefix
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemTextConversationActivityBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal class ConversationActivityTextMessageViewHolder(private val binding: NablaConversationTimelineItemTextConversationActivityBinding) : ChatViewHolder(binding.root) {
    fun bind(item: TimelineItem.ConversationActivity) {
        val text = when (item.content) {
            is TimelineItem.ConversationActivity.ProviderJoinedConversation -> {
                val providerText = when (item.content.maybeProvider) {
                    is DeletedProvider -> {
                        binding.context.getString(R.string.nabla_conversation_conversation_activity_deleted_provider_joined)
                    }
                    is Provider -> {
                        item.content.maybeProvider.fullNameWithPrefix(binding.context)
                    }
                }
                binding.context.getString(
                    R.string.nabla_conversation_conversation_activity_provider_joined,
                    providerText,
                )
            }
        }
        binding.conversationActivityTextView.text = text
    }

    companion object {
        fun create(inflater: LayoutInflater, parent: ViewGroup) = ConversationActivityTextMessageViewHolder(
            NablaConversationTimelineItemTextConversationActivityBinding.inflate(
                inflater,
                parent,
                false,
            ),
        )
    }
}
