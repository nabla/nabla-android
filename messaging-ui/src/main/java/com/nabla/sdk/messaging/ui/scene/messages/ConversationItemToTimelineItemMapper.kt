package com.nabla.sdk.messaging.ui.scene.messages

import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageSender

internal fun Message.toTimelineItem(
    showSenderAvatar: Boolean,
    showSenderName: Boolean,
    showStatus: Boolean,
): TimelineItem.Message {
    val copyActionOrNull = MessageAction.Copy.takeIf { this is Message.Text }
    val actions: Set<MessageAction> = when {
        message.sender is MessageSender.Provider -> setOfNotNull(copyActionOrNull)
        message.sender is MessageSender.System -> setOfNotNull(copyActionOrNull)
        message.sender is MessageSender.Patient && message.status.isSent -> setOfNotNull(copyActionOrNull, MessageAction.Delete)
        else -> emptySet()
    }
    return TimelineItem.Message(
        id = message.id,
        sender = message.sender,
        showSenderAvatar = showSenderAvatar,
        showSenderName = showSenderName,
        status = message.status,
        showStatus = showStatus,
        time = message.sentAt,
        actions = actions,
        content = toMessageContent(),
    )
}

private fun Message.toMessageContent() = when (this) {
    is Message.Deleted -> TimelineItem.Message.Deleted
    is Message.Media.Document -> TimelineItem.Message.File(
        uri = document.fileUpload.url.url,
        fileId = document.fileUpload.id,
        fileName = document.fileUpload.fileName,
        mimeType = document.fileUpload.mimeType,
        thumbnailUri = document.thumbnail?.fileUpload?.url?.url,
    )
    is Message.Media.Image -> TimelineItem.Message.Image(
        uri = image.fileUpload.url.url,
        fileName = image.fileUpload.fileName,
        mimeType = image.fileUpload.mimeType,
    )
    is Message.Text -> TimelineItem.Message.Text(
        text = text,
    )
}
