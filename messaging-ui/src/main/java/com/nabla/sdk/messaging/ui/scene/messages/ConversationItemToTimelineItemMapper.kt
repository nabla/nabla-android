package com.nabla.sdk.messaging.ui.scene.messages

import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivity
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivityContent
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.core.domain.entity.SendStatus

internal fun Message.toTimelineItem(
    showSenderAvatar: Boolean,
    showSenderName: Boolean,
    showStatus: Boolean,
    audioPlaybackProgressMap: Map<Uri, PlaybackProgress> = emptyMap(),
    nowPlayingAudioUri: Uri? = null,
): TimelineItem.Message {
    val copyActionOrNull = MessageAction.Copy.takeIf { this is Message.Text }
    val actions: Set<MessageAction> = when {
        this is Message.Deleted -> emptySet()
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
        content = toMessageContent(
            nowPlayingAudio = nowPlayingAudioUri,
            audioPlaybackProgressMap = audioPlaybackProgressMap,
        ),
    )
}

private fun Message.toMessageContent(
    nowPlayingAudio: Uri?,
    audioPlaybackProgressMap: Map<Uri, PlaybackProgress>,
) = when (this) {
    is Message.Deleted -> TimelineItem.Message.Deleted
    is Message.Media.Document -> TimelineItem.Message.File(
        uri = stableUri,
        fileName = fileName ?: "",
        mimeType = mimeType,
        thumbnailUri = thumbnailUri,
    )
    is Message.Media.Image -> TimelineItem.Message.Image(
        uri = stableUri,
    )
    is Message.Text -> TimelineItem.Message.Text(
        text = text,
    )
    is Message.Media.Audio -> TimelineItem.Message.Audio(
        uri = stableUri,
        isPlaying = nowPlayingAudio == stableUri,
        progress = audioPlaybackProgressMap[stableUri] ?: PlaybackProgress(currentPositionMillis = 0, durationMs),
    )
}

internal fun ConversationActivity.toTimelineItem(): TimelineItem.ConversationActivity {
    return when (val content = content) {
        is ConversationActivityContent.ProviderJoinedConversation -> {
            TimelineItem.ConversationActivity(
                createdAt,
                TimelineItem.ConversationActivity.ProviderJoinedConversation(content.maybeProvider)
            )
        }
    }
}
