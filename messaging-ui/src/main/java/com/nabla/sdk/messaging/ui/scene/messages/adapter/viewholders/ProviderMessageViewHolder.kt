package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.ui.helpers.abbreviatedNameWithPrefix
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.MessageContentBinder

internal sealed class ProviderMessageViewHolder<ContentType : TimelineItem.Message.Content, BinderType : MessageContentBinder<ContentType>>(
    private val binding: NablaConversationTimelineItemProviderMessageBinding,
    private val onProviderClicked: (providerId: Uuid) -> Unit,
    contentBinder: BinderType,
) : MessageViewHolder<ContentType, MessageSender.Provider, BinderType>(contentBinder, binding.root),
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

    override fun bind(message: TimelineItem.Message, sender: MessageSender.Provider, content: ContentType) {
        super.bind(message, sender, content)

        binding.chatProviderMessageAuthorTextView.isVisible = message.showSenderName
        binding.chatProviderMessageAvatarViewContainer.visibility = if (message.showSenderAvatar) VISIBLE else INVISIBLE

        val senderFullName = sender.provider.abbreviatedNameWithPrefix(binding.context)
        if (message.showSenderName) {
            binding.chatProviderMessageAuthorTextView.text = senderFullName
        }

        if (message.showSenderAvatar) {
            binding.chatProviderMessageAvatarView.loadAvatar(sender.provider)
            binding.chatProviderMessageAvatarViewContainer.setOnClickListener { sender.provider.id.let(onProviderClicked) }
        }
    }
}
