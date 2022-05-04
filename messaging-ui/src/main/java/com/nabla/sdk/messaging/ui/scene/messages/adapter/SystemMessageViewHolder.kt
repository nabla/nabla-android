package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemSystemMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.MessageContentBinder

internal sealed class SystemMessageViewHolder<ContentType : TimelineItem.Message.Content, BinderType : MessageContentBinder<ContentType>>(
    private val binding: NablaConversationTimelineItemSystemMessageBinding,
    contentBinder: BinderType,
) : MessageViewHolder<ContentType, MessageSender, BinderType>(contentBinder, binding.root),
    PopUpMenuHolder,
    ClickableItemHolder {

    override val clickableView: View
        get() = binding.chatSystemMessageContentContainer

    override val popUpMenu: PopupMenu = PopupMenu(
        binding.context,
        binding.chatSystemMessageContentContainer,
        Gravity.BOTTOM
    ).apply {
        menuInflater.inflate(R.menu.nabla_message_actions, menu)
    }

    override fun bind(message: TimelineItem.Message, sender: MessageSender, content: ContentType) {
        super.bind(message, sender, content)
        binding.chatSystemMessageAuthorTextView.isVisible = message.showSenderName
        binding.chatSystemMessageAvatarViewContainer.visibility = if (message.showSenderAvatar) VISIBLE else INVISIBLE
    }
}
