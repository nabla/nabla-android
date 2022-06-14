package com.nabla.sdk.messaging.ui.scene.messages

import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.core.domain.entity.MessageId

public data class RepliedMessage(
    val id: MessageId,
    val content: Content,
    val author: MessageAuthor,
) {
    public sealed interface Content {
        public data class Text(val text: String) : Content
        public data class Image(val uri: Uri) : Content
        public data class Audio(val uri: Uri) : Content
        public data class Document(val uri: Uri, val thumbnailUri: Uri?) : Content
        public object Deleted : Content
    }
}
