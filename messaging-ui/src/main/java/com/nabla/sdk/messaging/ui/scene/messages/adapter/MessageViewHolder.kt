package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.View
import androidx.annotation.CallSuper
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.MessageContentBinder

internal sealed class MessageViewHolder<ContentType, SenderType, BinderType>(
    val contentBinder: BinderType,
    itemView: View,
) : ChatViewHolder(itemView)
    where ContentType : TimelineItem.Message.Content, SenderType : MessageSender, BinderType : MessageContentBinder<ContentType> {

    @CallSuper
    open fun bind(message: TimelineItem.Message, sender: SenderType, content: ContentType) {
        contentBinder.bind(message.listItemId, content)
    }
}
