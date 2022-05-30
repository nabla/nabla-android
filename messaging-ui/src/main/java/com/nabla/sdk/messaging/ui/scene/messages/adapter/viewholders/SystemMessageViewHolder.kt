package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemSystemMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.MessageContentBinder

internal sealed class SystemMessageViewHolder<ContentType : TimelineItem.Message.Content, BinderType : MessageContentBinder<ContentType>>(
    private val binding: NablaConversationTimelineItemSystemMessageBinding,
    contentBinder: BinderType,
) : MessageViewHolder<ContentType, MessageAuthor.System, BinderType>(contentBinder, binding.root),
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

    override fun bind(message: TimelineItem.Message, author: MessageAuthor.System, content: ContentType) {
        super.bind(message, author, content)

        binding.chatSystemMessageAuthorTextView.isVisible = message.showAuthorName
        binding.chatSystemMessageAvatarView.visibility = if (message.showAuthorAvatar) VISIBLE else INVISIBLE

        if (message.showAuthorName) {
            binding.chatSystemMessageAuthorTextView.text = author.system.name
        }

        if (message.showAuthorAvatar) {
            binding.chatSystemMessageAvatarView.loadAvatar(author.system)
        }
    }
}
