package com.nabla.sdk.messaging.ui.scene.messages

import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.domain.entity.VideoCall
import com.nabla.sdk.core.domain.entity.VideoCallRoomStatus
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivity
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivityContent
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem.Message.Author

internal fun Message.toTimelineItem(
    showAuthorAvatar: Boolean,
    showAuthorName: Boolean,
    showStatus: Boolean,
    audioPlaybackProgressMap: Map<Uri, PlaybackProgress> = emptyMap(),
    nowPlayingAudioUri: Uri? = null,
    currentVideoCall: VideoCall?,
): TimelineItem {
    val copyActionOrNull = MessageAction.Copy.takeIf { this is Message.Text }
    val actions: Set<MessageAction> = if (this is Message.Deleted) emptySet() else {
        when (author) {
            is MessageAuthor.Patient.Current -> {
                if (sendStatus == SendStatus.Sent) {
                    setOfNotNull(copyActionOrNull, MessageAction.Delete, MessageAction.Reply)
                } else setOfNotNull(copyActionOrNull)
            }
            is MessageAuthor.Provider,
            is MessageAuthor.System,
            is MessageAuthor.Patient.Other,
            is MessageAuthor.DeletedProvider,
            is MessageAuthor.Unknown -> setOfNotNull(copyActionOrNull, MessageAction.Reply)
        }
    }
    return TimelineItem.Message(
        id = id,
        author = author.toTimelineMessageAuthor(),
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

private fun MessageAuthor.toTimelineMessageAuthor(): Author = when (this) {
    is MessageAuthor.Provider -> Author.Provider(provider)
    is MessageAuthor.Patient.Current -> Author.CurrentPatient
    is MessageAuthor.Patient.Other -> Author.Other(
        uuid = patient.id,
        displayName = patient.displayName,
        avatar = null,
    )
    is MessageAuthor.System -> Author.Other(
        uuid = null,
        displayName = system.name,
        avatar = system.avatar,
    )
    is MessageAuthor.DeletedProvider, MessageAuthor.Unknown -> Author.Other(
        uuid = null,
        displayName = "",
        avatar = null,
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
    is Message.VideoCallRoom -> {
        val status = when (val videoCallRoomStatus = videoCallRoom.status) {
            VideoCallRoomStatus.Closed -> TimelineItem.Message.LivekitRoom.Status.LivekitClosedRoom
            is VideoCallRoomStatus.Open -> TimelineItem.Message.LivekitRoom.Status.LivekitOpenedRoom(
                url = videoCallRoomStatus.url,
                token = videoCallRoomStatus.token,
                isCurrentVideoCall = videoCallRoom.id == currentVideoCall?.id,
            )
        }
        TimelineItem.Message.LivekitRoom(
            roomId = videoCallRoom.id,
            roomStatus = status,
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
        is Message.VideoCallRoom -> RepliedMessage.Content.LivekitRoom
    },
    author = author.toTimelineMessageAuthor(),
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
