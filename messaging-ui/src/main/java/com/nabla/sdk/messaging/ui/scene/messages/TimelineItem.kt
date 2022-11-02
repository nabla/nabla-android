package com.nabla.sdk.messaging.ui.scene.messages

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.DeletedProvider
import com.nabla.sdk.core.domain.entity.EphemeralUrl
import com.nabla.sdk.core.domain.entity.MaybeProvider
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import kotlinx.datetime.Instant
import com.nabla.sdk.core.domain.entity.Provider as CoreProvider

internal enum class MessageAction { Delete, Copy, Reply }

internal sealed interface TimelineItem {
    val listItemId: String

    data class Message(
        val id: MessageId,
        val author: Author,
        val showAuthorAvatar: Boolean,
        val showAuthorName: Boolean,
        val status: SendStatus,
        val showStatus: Boolean,
        val time: Instant,
        val actions: Set<MessageAction>,
        val content: Content,
    ) : TimelineItem {
        override val listItemId = "message_${id.stableId}"

        sealed interface Author {
            object CurrentPatient : Author
            data class Provider(val provider: CoreProvider) : Author

            // might be another patient, a system message or an unknown type.
            data class Other(
                val uuid: Uuid?,
                val displayName: String,
                val avatar: EphemeralUrl?,
            ) : Author
        }

        sealed interface Content {
            val repliedMessage: RepliedMessage?
        }

        data class Text(
            val text: String,
            override val repliedMessage: RepliedMessage?,
        ) : Content {
            internal companion object
        }

        data class Image(
            val uri: Uri,
        ) : Content {
            override val repliedMessage: RepliedMessage? = null

            internal companion object
        }

        data class Video(
            val uri: Uri,
        ) : Content {
            override val repliedMessage: RepliedMessage? = null

            internal companion object
        }

        data class File(
            val uri: Uri,
            val fileName: String,
            val mimeType: MimeType,
            val thumbnailUri: Uri?,
        ) : Content {
            override val repliedMessage: RepliedMessage? = null

            internal companion object
        }

        data class Audio(
            val uri: Uri,
            val progress: PlaybackProgress,
            val isPlaying: Boolean,
        ) : Content {
            override val repliedMessage: RepliedMessage? = null

            internal companion object
        }

        data class LivekitRoom(
            val roomId: Uuid,
            val roomStatus: Status,
        ) : Content {

            override val repliedMessage: RepliedMessage? = null

            sealed class Status {
                data class LivekitOpenedRoom(
                    val url: String,
                    val token: String,
                    val isCurrentVideoCall: Boolean,
                ) : Status()

                object LivekitClosedRoom : Status()
            }
        }

        object Deleted : Content {
            override val repliedMessage: RepliedMessage? = null
        }

        internal companion object
    }

    object LoadingMore : TimelineItem {
        override val listItemId = "loading_more"
    }

    data class DateSeparator(
        val date: Instant,
        override val listItemId: String,
    ) : TimelineItem

    data class ProviderTypingIndicator(
        val provider: CoreProvider,
        val showProviderName: Boolean,
    ) : TimelineItem {
        override val listItemId: String = "provider_typing_${provider.id}"
    }

    data class ConversationActivity(
        val date: Instant,
        val content: Content,
    ) : TimelineItem {

        sealed interface Content

        data class ProviderJoinedConversation(
            val maybeProvider: MaybeProvider,
        ) : Content

        override val listItemId = when (content) {
            is ProviderJoinedConversation -> {
                val id = when (content.maybeProvider) {
                    DeletedProvider -> "deleted_$date"
                    is CoreProvider -> "${content.maybeProvider.id}_$date"
                }
                "provider_joined_$id"
            }
        }
    }
}

internal fun TimelineItem.getDate(): Instant? = when (this) {
    is TimelineItem.Message -> time
    TimelineItem.LoadingMore -> null
    is TimelineItem.DateSeparator -> date
    is TimelineItem.ProviderTypingIndicator -> null
    is TimelineItem.ConversationActivity -> date
}
