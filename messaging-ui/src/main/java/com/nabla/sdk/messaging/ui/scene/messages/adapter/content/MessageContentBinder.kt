package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal sealed class MessageContentBinder<ContentType : TimelineItem.Message.Content> {
    abstract fun bind(messageId: String, item: ContentType)
}
