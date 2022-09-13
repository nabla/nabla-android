package com.nabla.sdk.messaging.ui.scene.messages

import com.nabla.sdk.core.domain.boundary.VideoCall
import com.nabla.sdk.core.domain.entity.LivekitRoomStatus
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
    currentVideoCall: VideoCall?,
): TimelineItem {
    val copyActionOrNull = MessageAction.Copy.takeIf { this is Message.Text }
    val actions: Set<MessageAction> = when {
        this is Message.Deleted -> emptySet()
        author is MessageAuthor.Provider -> setOfNotNull(copyActionOrNull, MessageAction.Reply)
        author is MessageAuthor.System -> setOfNotNull(copyActionOrNull, MessageAction.Reply)
        author is MessageAuthor.Patient && sendStatus == SendStatus.Sent ->
            setOfNotNull(copyActionOrNull, MessageAction.Delete, MessageAction.Reply)
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
            currentVideoCall = currentVideoCall,
        ),
    )
}

private fun Message.toMessageContent(
    nowPlayingAudio: Uri?,
    audioPlaybackProgressMap: Map<Uri, PlaybackProgress>,
    currentVideoCall: VideoCall?,
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
    is Message.Media.Video -> TimelineItem.Message.Video(
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
    is Message.LivekitRoom -> {
        val status = when (val livekitRoomStatus = livekitRoom.status) {
            LivekitRoomStatus.Closed -> TimelineItem.Message.LivekitRoom.Status.LivekitClosedRoom
            is LivekitRoomStatus.Open -> TimelineItem.Message.LivekitRoom.Status.LivekitOpenedRoom(
                url = livekitRoomStatus.url,
                token = livekitRoomStatus.token,
                isCurrentVideoCall = livekitRoom.id == currentVideoCall?.id
            )
        }
        TimelineItem.Message.LivekitRoom(
            roomId = livekitRoom.id,
            roomStatus = status
        )
    }
}

private fun Message.toRepliedMessage() = RepliedMessage(
    id = id,
    content = when (this) {
        is Message.Deleted -> RepliedMessage.Content.Deleted
        is Message.Media.Audio -> RepliedMessage.Content.Audio(stableUri)
        is Message.Media.Document -> RepliedMessage.Content.Document(stableUri, thumbnailUri)
        is Message.Media.Image -> RepliedMessage.Content.Image(stableUri)
        is Message.Media.Video -> RepliedMessage.Content.Video(stableUri)
        is Message.Text -> RepliedMessage.Content.Text(text)
        is Message.LivekitRoom -> RepliedMessage.Content.LivekitRoom
    },
    author = author,
)

internal fun TimelineItem.Message.toRepliedMessage() = RepliedMessage(
    id = id,
    content = when (content) {
        is TimelineItem.Message.Deleted -> RepliedMessage.Content.Deleted
        is TimelineItem.Message.Audio -> RepliedMessage.Content.Audio(content.uri)
        is TimelineItem.Message.File -> RepliedMessage.Content.Document(content.uri, content.thumbnailUri)
        is TimelineItem.Message.Image -> RepliedMessage.Content.Image(content.uri)
        is TimelineItem.Message.Video -> RepliedMessage.Content.Video(content.uri)
        is TimelineItem.Message.Text -> RepliedMessage.Content.Text(content.text)
        is TimelineItem.Message.LivekitRoom -> RepliedMessage.Content.LivekitRoom
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
