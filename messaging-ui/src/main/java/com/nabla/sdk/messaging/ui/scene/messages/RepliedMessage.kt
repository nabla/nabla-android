package com.nabla.sdk.messaging.ui.scene.messages

import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.domain.entity.MessageId

internal data class RepliedMessage(
    val id: MessageId,
    val content: Content,
    val author: TimelineItem.Message.Author,
) {
    sealed interface Content {
        data class Text(val text: String) : Content
        data class Image(val uri: Uri) : Content
        data class Video(val uri: Uri) : Content
        data class Audio(val uri: Uri) : Content
        data class Document(val uri: Uri, val thumbnailUri: Uri?) : Content
        object Deleted : Content
        object LivekitRoom : Content
    }
}
