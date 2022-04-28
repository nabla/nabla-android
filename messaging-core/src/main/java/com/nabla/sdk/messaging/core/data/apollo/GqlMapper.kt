package com.nabla.sdk.messaging.core.data.apollo

import com.nabla.sdk.core.domain.entity.BaseFileUpload
import com.nabla.sdk.core.domain.entity.EphemeralUrl
import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Size
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.graphql.fragment.ConversationFragment
import com.nabla.sdk.graphql.fragment.DocumentFileUploadFragment
import com.nabla.sdk.graphql.fragment.EphemeralUrlFragment
import com.nabla.sdk.graphql.fragment.ImageFileUploadFragment
import com.nabla.sdk.graphql.fragment.MessageFragment
import com.nabla.sdk.graphql.fragment.ProviderFragment
import com.nabla.sdk.graphql.fragment.ProviderInConversationFragment
import com.nabla.sdk.messaging.core.domain.entity.BaseMessage
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.domain.entity.toConversationId

internal class GqlMapper {
    fun mapToConversation(fragment: ConversationFragment): Conversation {
        return Conversation(
            id = fragment.id.toConversationId(),
            title = fragment.title,
            description = fragment.description,
            lastMessagePreview = fragment.lastMessagePreview,
            lastModified = fragment.updatedAt,
            patientUnreadMessageCount = fragment.unreadMessageCount,
            providersInConversation = fragment.providers.map {
                mapToProviderInConversation(it.providerInConversationFragment)
            }
        )
    }

    private fun mapToProviderInConversation(
        fragment: ProviderInConversationFragment
    ): ProviderInConversation {
        return ProviderInConversation(
            provider = mapToProvider(fragment.provider.providerFragment),
            typingAt = fragment.typingAt,
            seenUntil = fragment.seenUntil,
        )
    }

    fun mapToMessage(messageFragment: MessageFragment, sendStatus: SendStatus): Message {
        val sender = mapToMessageSender(messageFragment.author)
        val baseMessage = BaseMessage(
            id = MessageId.Remote(
                clientId = messageFragment.clientId,
                remoteId = messageFragment.id
            ),
            sentAt = messageFragment.createdAt,
            sender = sender,
            sendStatus = sendStatus,
            conversationId = messageFragment.conversation.id.toConversationId(),
        )
        messageFragment.content?.messageContentFragment?.onTextMessageContent?.textMessageContentFragment?.let {
            return Message.Text(
                baseMessage = baseMessage,
                text = it.text
            )
        }
        messageFragment.content?.messageContentFragment?.onImageMessageContent?.imageMessageContentFragment?.let {
            return Message.Media.Image(
                baseMessage = baseMessage,
                mediaSource = FileSource.Uploaded(
                    fileLocal = null,
                    fileUpload = mapToFileUploadImage(it.imageFileUpload.imageFileUploadFragment)
                ),
            )
        }
        messageFragment.content?.messageContentFragment?.onDocumentMessageContent?.documentMessageContentFragment?.let {
            return Message.Media.Document(
                baseMessage = baseMessage,
                mediaSource = FileSource.Uploaded(
                    fileLocal = null,
                    fileUpload = mapToFileUploadDocument(it.documentFileUpload.documentFileUploadFragment)
                )
            )
        }
        error {
            "Unknown message content mapping for $messageFragment"
        }
    }

    private fun mapToMessageSender(author: MessageFragment.Author): MessageSender {
        author.onPatient?.let { return MessageSender.Patient }
        author.onProvider?.providerFragment?.let { return MessageSender.Provider(mapToProvider(it)) }
        author.onSystem?.let { return MessageSender.System }
        author.onDeletedProvider?.let { return MessageSender.DeletedProvider }
        return MessageSender.Unknown
    }

    private fun mapToProvider(providerFragment: ProviderFragment): User.Provider {
        val avatarUrl = providerFragment.avatarUrl?.ephemeralUrlFragment?.let { mapToEphemeralUrl(it) }
        return User.Provider(
            id = providerFragment.id,
            avatar = avatarUrl,
            firstName = "", // TODO : Add to GQL
            lastName = "", // TODO : Add to GQL
            title = null, // TODO : Add to GQL
            prefix = null, // TODO : Add to GQL
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
        imageFileUploadFragment: ImageFileUploadFragment
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
        documentFileUploadFragment: DocumentFileUploadFragment
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
}
