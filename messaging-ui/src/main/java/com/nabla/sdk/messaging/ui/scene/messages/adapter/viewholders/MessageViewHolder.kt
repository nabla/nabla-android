package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.View
import androidx.annotation.CallSuper
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.MessageContentBinder

internal sealed class MessageViewHolder<ContentType, AuthorType, BinderType>(
    val contentBinder: BinderType,
    itemView: View,
) : ChatViewHolder(itemView)
    where ContentType : TimelineItem.Message.Content, AuthorType : TimelineItem.Message.Author, BinderType : MessageContentBinder<ContentType> {

    @CallSuper
    open fun bind(message: TimelineItem.Message, author: AuthorType, content: ContentType) {
        contentBinder.bind(message.listItemId, content)
    }
}
