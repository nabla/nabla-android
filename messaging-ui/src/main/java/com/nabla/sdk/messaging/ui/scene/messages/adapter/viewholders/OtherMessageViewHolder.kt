package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemOtherMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.MessageContentBinder

internal sealed class OtherMessageViewHolder<ContentType : TimelineItem.Message.Content, BinderType : MessageContentBinder<ContentType>>(
    private val binding: NablaConversationTimelineItemOtherMessageBinding,
    contentBinder: BinderType,
) : MessageViewHolder<ContentType, TimelineItem.Message.Author.Other, BinderType>(contentBinder, binding.root),
    PopUpMenuHolder,
    ClickableItemHolder {

    override val clickableView: View
        get() = binding.chatOtherMessageContentContainer

    override val popUpMenu: PopupMenu = PopupMenu(
        binding.context,
        binding.chatOtherMessageContentContainer,
        Gravity.BOTTOM
    ).apply {
        menuInflater.inflate(R.menu.nabla_message_actions, menu)
    }

    override fun bind(message: TimelineItem.Message, author: TimelineItem.Message.Author.Other, content: ContentType) {
        super.bind(message, author, content)

        binding.chatOtherMessageAuthorTextView.isVisible = message.showAuthorName && author.displayName.isNotEmpty()
        binding.chatOtherMessageAvatarView.visibility = if (message.showAuthorAvatar) VISIBLE else INVISIBLE

        binding.chatOtherMessageAuthorTextView.text = if (message.showAuthorName) author.displayName else null

        if (message.showAuthorAvatar) {
            binding.chatOtherMessageAvatarView.loadAvatar(author.avatar?.url, author.displayName.firstOrNull()?.toString(), author.uuid)
        } else binding.chatOtherMessageAvatarView.displayUnicolorPlaceholder()
    }
}
