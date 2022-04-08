package com.nabla.sdk.messaging.ui.scene.messages

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.core.domain.entity.MessageStatus
import kotlinx.datetime.Instant

internal enum class MessageAction { Delete, Copy }

internal sealed interface TimelineItem {
    val listItemId: String

    data class Message(
        val id: MessageId,
        val sender: MessageSender,
        val showSenderAvatar: Boolean,
        val showSenderName: Boolean,
        val status: MessageStatus,
        val showStatus: Boolean,
        val time: Instant,
        val actions: Set<MessageAction>,
        val content: Content,
    ) : TimelineItem {
        override val listItemId = "message_${id.stableId}"

        sealed interface Content

        data class Text(
            val text: String,
        ) : Content

        data class Image(
            val uri: Uri,
            val mimeType: MimeType,
            val fileName: String,
        ) : Content

        data class File(
            val uri: Uri,
            val fileId: Uuid?,
            val fileName: String,
            val mimeType: MimeType,
            val thumbnailUri: Uri?,
        ) : Content

        object Deleted : Content
    }

    object LoadingMore : TimelineItem {
        override val listItemId = "load_more"
    }

    data class DateSeparator(
        val date: Instant,
        override val listItemId: String,
    ) : TimelineItem

    data class ProviderTypingIndicator(
        val provider: User.Provider,
        val showProviderName: Boolean,
    ) : TimelineItem {
        override val listItemId: String = "provider_typing_${provider.id}"
    }
}

internal fun TimelineItem.getDate(): Instant? = when (this) {
    is TimelineItem.Message -> time
    TimelineItem.LoadingMore -> null
    is TimelineItem.DateSeparator -> this.date
    is TimelineItem.ProviderTypingIndicator -> null
}
