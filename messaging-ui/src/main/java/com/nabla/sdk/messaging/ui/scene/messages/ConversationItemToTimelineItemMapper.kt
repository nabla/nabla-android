package com.nabla.sdk.messaging.ui.scene.messages

import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivity
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivityContent
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.core.domain.entity.SendStatus

internal fun Message.toTimelineItem(
    showAuthorAvatar: Boolean,
    showAuthorName: Boolean,
    showStatus: Boolean,
    audioPlaybackProgressMap: Map<Uri, PlaybackProgress> = emptyMap(),
    nowPlayingAudioUri: Uri? = null,
): TimelineItem.Message {
    val copyActionOrNull = MessageAction.Copy.takeIf { this is Message.Text }
    val actions: Set<MessageAction> = when {
        this is Message.Deleted -> emptySet()
        author is MessageAuthor.Provider -> setOfNotNull(copyActionOrNull)
        author is MessageAuthor.System -> setOfNotNull(copyActionOrNull)
        author is MessageAuthor.Patient && sendStatus == SendStatus.Sent -> setOfNotNull(copyActionOrNull, MessageAction.Delete)
        else -> emptySet()
    }
    return TimelineItem.Message(
        id = id,
        author = author,
        showAuthorAvatar = showAuthorAvatar,
        showAuthorName = showAuthorName,
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
): TimelineItem.Message.Content = when (this) {
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
        repliedMessage = replyTo?.toRepliedMessage(),
    )
    is Message.Media.Audio -> TimelineItem.Message.Audio(
        uri = stableUri,
        isPlaying = nowPlayingAudio == stableUri,
        progress = audioPlaybackProgressMap[stableUri] ?: PlaybackProgress(currentPositionMillis = 0, durationMs),
    )
}

private fun Message.toRepliedMessage() = RepliedMessage(
    id = id,
    content = when (this) {
        is Message.Deleted -> RepliedMessage.Content.Deleted
        is Message.Media.Audio -> RepliedMessage.Content.Audio(stableUri)
        is Message.Media.Document -> RepliedMessage.Content.Document(stableUri)
        is Message.Media.Image -> RepliedMessage.Content.Image(stableUri)
        is Message.Text -> RepliedMessage.Content.Text(text)
    },
    author = author,
)

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
