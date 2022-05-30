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
import com.nabla.sdk.graphql.fragment.AudioFileUploadFragment
import com.nabla.sdk.graphql.fragment.ConversationActivityContentFragment
import com.nabla.sdk.graphql.fragment.ConversationActivityFragment
import com.nabla.sdk.graphql.fragment.ConversationFragment
import com.nabla.sdk.graphql.fragment.DocumentFileUploadFragment
import com.nabla.sdk.graphql.fragment.EphemeralUrlFragment
import com.nabla.sdk.graphql.fragment.ImageFileUploadFragment
import com.nabla.sdk.graphql.fragment.MaybeProviderFragment
import com.nabla.sdk.graphql.fragment.MessageFragment
import com.nabla.sdk.graphql.fragment.ProviderFragment
import com.nabla.sdk.graphql.fragment.ProviderInConversationFragment
import com.nabla.sdk.graphql.fragment.SystemFragment
import com.nabla.sdk.messaging.core.domain.entity.BaseMessage
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivity
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivityContent
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.domain.entity.toConversationActivityId
import com.nabla.sdk.messaging.core.domain.entity.toConversationId

internal class GqlMapper(private val logger: Logger) {
    fun mapToConversation(fragment: ConversationFragment): Conversation {
        return Conversation(
            id = fragment.id.toConversationId(),
            title = fragment.title,
            description = fragment.description,
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
            logger.warn("Unknown conversation activity content mapping for $conversationActivityFragment")
            return null
        }
        return ConversationActivity(
            id = conversationActivityFragment.id.toConversationActivityId(),
            conversationId = conversationActivityFragment.conversation.id.toConversationId(),
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

    fun mapToMessage(messageFragment: MessageFragment, sendStatus: SendStatus): Message? {
        val author = mapToMessageAuthor(messageFragment.author)
        val baseMessage = BaseMessage(
            id = MessageId.Remote(
                clientId = messageFragment.clientId,
                remoteId = messageFragment.id
            ),
            createdAt = messageFragment.createdAt,
            author = author,
            sendStatus = sendStatus,
            conversationId = messageFragment.conversation.id.toConversationId(),
        )
        messageFragment.messageContent.messageContentFragment.onTextMessageContent?.textMessageContentFragment?.let {
            return Message.Text(
                baseMessage = baseMessage,
                text = it.text,
            )
        }
        messageFragment.messageContent.messageContentFragment.onImageMessageContent?.imageMessageContentFragment?.let {
            return Message.Media.Image(
                baseMessage = baseMessage,
                mediaSource = FileSource.Uploaded(
                    fileLocal = null,
                    fileUpload = mapToFileUploadImage(it.imageFileUpload.imageFileUploadFragment),
                ),
            )
        }
        messageFragment.messageContent.messageContentFragment.onDocumentMessageContent?.documentMessageContentFragment?.let {
            return Message.Media.Document(
                baseMessage = baseMessage,
                mediaSource = FileSource.Uploaded(
                    fileLocal = null,
                    fileUpload = mapToFileUploadDocument(it.documentFileUpload.documentFileUploadFragment),
                )
            )
        }
        messageFragment.messageContent.messageContentFragment.onAudioMessageContent?.audioMessageContentFragment?.let {
            return Message.Media.Audio(
                baseMessage = baseMessage,
                mediaSource = FileSource.Uploaded(
                    fileLocal = null,
                    fileUpload = mapToFileUploadAudio(it.audioFileUpload.audioFileUploadFragment),
                )
            )
        }
        messageFragment.messageContent.messageContentFragment.onDeletedMessageContent?.let {
            return Message.Deleted(baseMessage = baseMessage)
        }
        logger.warn("Unknown message content mapping for $messageFragment")
        return null
    }

    private fun mapToMessageAuthor(author: MessageFragment.Author): MessageAuthor {
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

    private fun ImageFileUploadFragment.size(): Size? {
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
}
