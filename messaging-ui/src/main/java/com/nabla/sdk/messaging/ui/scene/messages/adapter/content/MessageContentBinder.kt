package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import androidx.annotation.AttrRes
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal sealed class MessageContentBinder<ContentType : TimelineItem.Message.Content>(@AttrRes protected val contentColorAttr: Int) {
    abstract fun bind(messageId: String, item: ContentType)
}
