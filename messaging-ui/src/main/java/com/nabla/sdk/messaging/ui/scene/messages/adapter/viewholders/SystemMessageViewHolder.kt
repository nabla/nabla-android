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
) : MessageViewHolder<ContentType, MessageAuthor, BinderType>(contentBinder, binding.root),
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

    // we can't assume author is System because System VH is used as fallback when author is unknown/deleted/etc.
    override fun bind(message: TimelineItem.Message, author: MessageAuthor, content: ContentType) {
        super.bind(message, author, content)

        binding.chatSystemMessageAuthorTextView.isVisible = message.showAuthorName
        binding.chatSystemMessageAvatarView.visibility = if (message.showAuthorAvatar) VISIBLE else INVISIBLE

        binding.chatSystemMessageAuthorTextView.text =
            if (message.showAuthorName && author is MessageAuthor.System) {
                author.system.name
            } else ""

        if (message.showAuthorAvatar && author is MessageAuthor.System) {
            binding.chatSystemMessageAvatarView.loadAvatar(author.system)
        } else binding.chatSystemMessageAvatarView.displayUnicolorPlaceholder()
    }
}
