package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.ui.helpers.MessageAuthorExtensions.abbreviatedNameWithPrefix
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.MessageContentBinder

internal sealed class ProviderMessageViewHolder<ContentType : TimelineItem.Message.Content, BinderType : MessageContentBinder<ContentType>>(
    private val binding: NablaConversationTimelineItemProviderMessageBinding,
    private val onProviderClicked: (providerId: Uuid) -> Unit,
    contentBinder: BinderType,
) : MessageViewHolder<ContentType, TimelineItem.Message.Author.Provider, BinderType>(contentBinder, binding.root),
    ClickableItemHolder,
    PopUpMenuHolder {

    override val clickableView: View
        get() = binding.chatProviderMessageContentContainer

    override val popUpMenu: PopupMenu = PopupMenu(
        binding.context,
        binding.chatProviderMessageContentContainer,
        Gravity.BOTTOM,
    ).apply {
        menuInflater.inflate(R.menu.nabla_message_actions, menu)
    }

    override fun bind(message: TimelineItem.Message, author: TimelineItem.Message.Author.Provider, content: ContentType) {
        super.bind(message, author, content)

        binding.chatProviderMessageAuthorTextView.isVisible = message.showAuthorName
        binding.chatProviderMessageAvatarViewContainer.visibility = if (message.showAuthorAvatar) VISIBLE else INVISIBLE

        val authorFullName = author.provider.abbreviatedNameWithPrefix(binding.context)
        if (message.showAuthorName) {
            binding.chatProviderMessageAuthorTextView.text = authorFullName
        }

        if (message.showAuthorAvatar) {
            binding.chatProviderMessageAvatarView.loadAvatar(author.provider)
            binding.chatProviderMessageAvatarViewContainer.setOnClickListener { author.provider.id.let(onProviderClicked) }
        }
    }
}
