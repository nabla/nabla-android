package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.graphics.drawable.AnimatedVectorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.nabla.sdk.core.ui.helpers.MessageAuthorExtensions.fullNameWithPrefix
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageBinding
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageContentBinding
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderTypingIndicatorBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal class ProviderTypingIndicatorViewHolder(private val binding: NablaConversationTimelineItemProviderMessageBinding) :
    ChatViewHolder(binding.root) {

    private val dancingDots: NablaConversationTimelineItemProviderTypingIndicatorBinding

    init {
        val inflater = LayoutInflater.from(binding.context)
        val contentBinding =
            NablaConversationTimelineItemProviderMessageContentBinding.inflate(inflater, binding.chatProviderMessageContentContainer, true)
        dancingDots = NablaConversationTimelineItemProviderTypingIndicatorBinding.inflate(inflater, contentBinding.chatProviderMessageContent, true)
    }

    fun bind(item: TimelineItem.ProviderTypingIndicator) {
        val provider = item.provider
        val context = binding.context
        binding.chatProviderMessageAvatarView.loadAvatar(provider)

        binding.chatProviderMessageAuthorTextView.isVisible = item.showProviderName
        binding.chatProviderMessageAuthorTextView.text = provider.fullNameWithPrefix(context)

        (dancingDots.dots.drawable as? AnimatedVectorDrawable)?.start()
    }

    companion object {
        fun create(inflater: LayoutInflater, parent: ViewGroup) = ProviderTypingIndicatorViewHolder(
            NablaConversationTimelineItemProviderMessageBinding.inflate(inflater, parent, false)
        )
    }
}
