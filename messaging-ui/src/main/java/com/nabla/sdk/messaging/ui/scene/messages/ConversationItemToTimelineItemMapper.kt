package com.nabla.sdk.messaging.ui.scene.messages

import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.core.domain.entity.SendStatus

internal fun Message.toTimelineItem(
    showSenderAvatar: Boolean,
    showSenderName: Boolean,
    showStatus: Boolean,
): TimelineItem.Message {
    val copyActionOrNull = MessageAction.Copy.takeIf { this is Message.Text }
    val actions: Set<MessageAction> = when {
        sender is MessageSender.Provider -> setOfNotNull(copyActionOrNull)
        sender is MessageSender.System -> setOfNotNull(copyActionOrNull)
        sender is MessageSender.Patient && sendStatus == SendStatus.Sent -> setOfNotNull(copyActionOrNull, MessageAction.Delete)
        else -> emptySet()
    }
    return TimelineItem.Message(
        id = id,
        sender = sender,
        showSenderAvatar = showSenderAvatar,
        showSenderName = showSenderName,
        status = sendStatus,
        showStatus = showStatus,
        time = sentAt,
        actions = actions,
        content = toMessageContent(),
    )
}

private fun Message.toMessageContent() = when (this) {
    is Message.Deleted -> TimelineItem.Message.Deleted
    is Message.Media.Document -> TimelineItem.Message.File(
        uri = uri,
        fileName = documentName ?: "",
        mimeType = mimeType,
        thumbnailUri = thumbnailUri,
    )
    is Message.Media.Image -> TimelineItem.Message.Image(
        uri = stableUri,
    )
    is Message.Text -> TimelineItem.Message.Text(
        text = text,
    )
}
