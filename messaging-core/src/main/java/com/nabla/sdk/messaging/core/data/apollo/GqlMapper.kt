package com.nabla.sdk.messaging.core.data.apollo

import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.BaseFileUpload
import com.nabla.sdk.core.domain.entity.DeletedProvider
import com.nabla.sdk.core.domain.entity.EphemeralUrl
import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.MaybeProvider
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.Size
import com.nabla.sdk.core.domain.entity.SystemUser
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.data.conversation.LocalConversationDataSource
import com.nabla.sdk.messaging.core.domain.entity.BaseMessage
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivity
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivityContent
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.LivekitRoomStatus
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.domain.entity.toConversationActivityId
import com.nabla.sdk.messaging.graphql.fragment.AudioFileUploadFragment
import com.nabla.sdk.messaging.graphql.fragment.ConversationActivityContentFragment
import com.nabla.sdk.messaging.graphql.fragment.ConversationActivityFragment
import com.nabla.sdk.messaging.graphql.fragment.ConversationFragment
import com.nabla.sdk.messaging.graphql.fragment.DocumentFileUploadFragment
import com.nabla.sdk.messaging.graphql.fragment.EphemeralUrlFragment
import com.nabla.sdk.messaging.graphql.fragment.ImageFileUploadFragment
import com.nabla.sdk.messaging.graphql.fragment.LivekitRoomMessageContentFragment
import com.nabla.sdk.messaging.graphql.fragment.MaybeProviderFragment
import com.nabla.sdk.messaging.graphql.fragment.MessageSummaryFragment
import com.nabla.sdk.messaging.graphql.fragment.ProviderFragment
import com.nabla.sdk.messaging.graphql.fragment.ProviderInConversationFragment
import com.nabla.sdk.messaging.graphql.fragment.SystemFragment
import com.nabla.sdk.messaging.graphql.fragment.VideoFileUploadFragment

internal class GqlMapper(
    private val logger: Logger,
    private val localConversationDataSource: LocalConversationDataSource,
) {
    fun mapToConversation(fragment: ConversationFragment): Conversation {
        return Conversation(
            id = localConversationDataSource.findLocalConversationId(fragment.id),
            title = fragment.title,
            subtitle = fragment.subtitle,
            inboxPreviewTitle = fragment.inboxPreviewTitle,
            lastMessagePreview = fragment.lastMessagePreview,
            lastModified = fragment.updatedAt,
            patientUnreadMessageCount = fragment.unreadMessageCount,
            providersInConversation = fragment.providers.map {
                mapToProviderInConversation(it.providerInConversationFragment)
            }
        )
    }

    fun mapToProviderInConversation(
        fragment: ProviderInConversationFragment,
    ): ProviderInConversation {
        return ProviderInConversation(
            provider = mapToProvider(fragment.provider.providerFragment),
            typingAt = fragment.typingAt,
            seenUntil = fragment.seenUntil,
        )
    }

    fun mapToConversationActivity(
        conversationActivityFragment: ConversationActivityFragment,
    ): ConversationActivity? {
        val content = mapToConversationActivityContent(conversationActivityFragment.conversationActivityContent.conversationActivityContentFragment)
        if (content == null) {
            logger.error("Unknown conversation activity content mapping for $conversationActivityFragment")
            return null
        }
        return ConversationActivity(
            id = conversationActivityFragment.id.toConversationActivityId(),
            conversationId = localConversationDataSource.findLocalConversationId(conversationActivityFragment.conversation.id),
            createdAt = conversationActivityFragment.createdAt,
            activityTime = conversationActivityFragment.activityTime,
            content = content,
        )
    }

    private fun mapToConversationActivityContent(
        conversationActivityContentFragment: ConversationActivityContentFragment,
    ): ConversationActivityContent? {
        conversationActivityContentFragment.onProviderJoinedConversation?.let {
            val maybeProvider = mapToMaybeProvider(it.provider.maybeProviderFragment) ?: return null
            return ConversationActivityContent.ProviderJoinedConversation(maybeProvider)
        }
        return null
    }

    private fun mapToMaybeProvider(maybeProviderFragment: MaybeProviderFragment): MaybeProvider? {
        maybeProviderFragment.onProvider?.let { return mapToProvider(it.providerFragment) }
        maybeProviderFragment.onDeletedProvider?.let { return DeletedProvider }
        return null
    }

    fun mapToMessage(
        summaryFragment: MessageSummaryFragment,
        sendStatus: SendStatus,
        replyTo: MessageSummaryFragment?,
    ): Message? {
        val baseMessage = mapToBaseMessage(summaryFragment, sendStatus, replyTo)

        summaryFragment.messageContent.messageContentFragment.onTextMessageContent?.textMessageContentFragment?.let {
            return Message.Text(
                baseMessage = baseMessage,
                text = it.text,
            )
        }
        summaryFragment.messageContent.messageContentFragment.onImageMessageContent?.imageMessageContentFragment?.let {
            return Message.Media.Image(
                baseMessage = baseMessage,
                mediaSource = FileSource.Uploaded(
                    fileLocal = null,
                    fileUpload = mapToFileUploadImage(it.imageFileUpload.imageFileUploadFragment),
                ),
            )
        }
        summaryFragment.messageContent.messageContentFragment.onVideoMessageContent?.videoMessageContentFragment?.let {
            return Message.Media.Video(
                baseMessage = baseMessage,
                mediaSource = FileSource.Uploaded(
                    fileLocal = null,
                    fileUpload = mapToFileUploadVideo(it.videoFileUpload.videoFileUploadFragment),
                ),
            )
        }
        summaryFragment.messageContent.messageContentFragment.onDocumentMessageContent?.documentMessageContentFragment?.let {
            return Message.Media.Document(
                baseMessage = baseMessage,
                mediaSource = FileSource.Uploaded(
                    fileLocal = null,
                    fileUpload = mapToFileUploadDocument(it.documentFileUpload.documentFileUploadFragment),
                )
            )
        }
        summaryFragment.messageContent.messageContentFragment.onAudioMessageContent?.audioMessageContentFragment?.let {
            return Message.Media.Audio(
                baseMessage = baseMessage,
                mediaSource = FileSource.Uploaded(
                    fileLocal = null,
                    fileUpload = mapToFileUploadAudio(it.audioFileUpload.audioFileUploadFragment),
                )
            )
        }
        summaryFragment.messageContent.messageContentFragment.onLivekitRoomMessageContent?.let {
            val livekitRoomId = it.livekitRoomMessageContentFragment.livekitRoom.uuid
            val livekitRoomStatus = mapToLivekitRoomStatus(it.livekitRoomMessageContentFragment.livekitRoom.status)
            livekitRoomStatus?.let {
                return Message.LivekitRoom(
                    baseMessage = baseMessage,
                    livekitRoomId = livekitRoomId,
                    livekitRoomStatus = livekitRoomStatus
                )
            }
        }
        summaryFragment.messageContent.messageContentFragment.onDeletedMessageContent?.let {
            return Message.Deleted(baseMessage = baseMessage)
        }
        logger.error("Unknown message content mapping for $summaryFragment")
        return null
    }

    private fun mapToBaseMessage(
        summaryFragment: MessageSummaryFragment,
        sendStatus: SendStatus,
        replyTo: MessageSummaryFragment?,
    ): BaseMessage {
        val author = mapToMessageAuthor(summaryFragment.author)
        return BaseMessage(
            id = MessageId.Remote(
                clientId = summaryFragment.clientId,
                remoteId = summaryFragment.id
            ),
            createdAt = summaryFragment.createdAt,
            author = author,
            sendStatus = sendStatus,
            replyTo = replyTo?.let {
                mapToMessage(
                    summaryFragment = replyTo,
                    sendStatus = SendStatus.Sent,
                    replyTo = null,
                )
            }
        )
    }

    private fun mapToMessageAuthor(author: MessageSummaryFragment.Author): MessageAuthor {
        author.onPatient?.let { return MessageAuthor.Patient }
        author.onProvider?.providerFragment?.let { return MessageAuthor.Provider(mapToProvider(it)) }
        author.onSystem?.let { return MessageAuthor.System(mapToSystem(it.systemFragment)) }
        author.onDeletedProvider?.let { return MessageAuthor.DeletedProvider }
        return MessageAuthor.Unknown
    }

    private fun mapToProvider(providerFragment: ProviderFragment): Provider {
        val avatarUrl = providerFragment.avatarUrl?.ephemeralUrlFragment?.let { mapToEphemeralUrl(it) }
        return Provider(
            id = providerFragment.id,
            avatar = avatarUrl,
            firstName = providerFragment.firstName,
            lastName = providerFragment.lastName,
            prefix = providerFragment.prefix,
        )
    }

    private fun mapToSystem(systemFragment: SystemFragment): SystemUser {
        val avatarUrl = systemFragment.avatar?.url?.ephemeralUrlFragment?.let { mapToEphemeralUrl(it) }
        return SystemUser(
            name = systemFragment.name,
            avatar = avatarUrl,
        )
    }

    private fun mapToEphemeralUrl(ephemeralUrlFragment: EphemeralUrlFragment): EphemeralUrl {
        return EphemeralUrl(
            expiresAt = ephemeralUrlFragment.expiresAt,
            url = Uri(ephemeralUrlFragment.url)
        )
    }

    private fun mapToMimeType(value: String): MimeType {
        return MimeType.fromStringRepresentation(value)
    }

    private fun mapToFileUploadImage(
        imageFileUploadFragment: ImageFileUploadFragment,
    ): FileUpload.Image {
        return FileUpload.Image(
            size = imageFileUploadFragment.size(),
            fileUpload = BaseFileUpload(
                id = imageFileUploadFragment.id,
                url = mapToEphemeralUrl(imageFileUploadFragment.url.ephemeralUrlFragment),
                fileName = imageFileUploadFragment.fileName,
                mimeType = mapToMimeType(imageFileUploadFragment.mimeType)
            )
        )
    }

    private fun mapToFileUploadVideo(
        videoFileUploadFragment: VideoFileUploadFragment,
    ): FileUpload.Video {
        return FileUpload.Video(
            size = videoFileUploadFragment.size(),
            durationMs = videoFileUploadFragment.durationMs?.toLong(),
            fileUpload = BaseFileUpload(
                id = videoFileUploadFragment.id,
                url = mapToEphemeralUrl(videoFileUploadFragment.url.ephemeralUrlFragment),
                fileName = videoFileUploadFragment.fileName,
                mimeType = mapToMimeType(videoFileUploadFragment.mimeType)
            )
        )
    }

    private fun ImageFileUploadFragment.size(): Size? {
        return if (width != null && height != null) Size(width, height) else null
    }

    private fun VideoFileUploadFragment.size(): Size? {
        return if (width != null && height != null) Size(width, height) else null
    }

    private fun mapToFileUploadDocument(
        documentFileUploadFragment: DocumentFileUploadFragment,
    ): FileUpload.Document {
        return FileUpload.Document(
            thumbnail = documentFileUploadFragment.thumbnail?.imageFileUploadFragment?.let {
                mapToFileUploadImage(it)
            },
            fileUpload = BaseFileUpload(
                id = documentFileUploadFragment.id,
                url = mapToEphemeralUrl(documentFileUploadFragment.url.ephemeralUrlFragment),
                fileName = documentFileUploadFragment.fileName,
                mimeType = mapToMimeType(documentFileUploadFragment.mimeType)
            )
        )
    }

    private fun mapToFileUploadAudio(
        audioFileUploadFragment: AudioFileUploadFragment,
    ): FileUpload.Audio {
        return FileUpload.Audio(
            durationMs = audioFileUploadFragment.durationMs?.toLong(),
            fileUpload = BaseFileUpload(
                id = audioFileUploadFragment.id,
                url = mapToEphemeralUrl(audioFileUploadFragment.url.ephemeralUrlFragment),
                fileName = audioFileUploadFragment.fileName,
                mimeType = mapToMimeType(audioFileUploadFragment.mimeType)
            )
        )
    }

    private fun mapToLivekitRoomStatus(status: LivekitRoomMessageContentFragment.Status): LivekitRoomStatus? {
        status.onLivekitRoomClosedStatus?.let {
            return LivekitRoomStatus.Closed
        }
        status.onLivekitRoomOpenStatus?.let {
            return LivekitRoomStatus.Open(
                url = it.url,
                token = it.token
            )
        }
        logger.error("Unknown livekit room status mapping for $status")
        return null
    }
}
