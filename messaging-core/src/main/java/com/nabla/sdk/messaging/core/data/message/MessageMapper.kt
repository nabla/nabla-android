package com.nabla.sdk.messaging.core.data.message

import com.apollographql.apollo3.api.Optional
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.graphql.type.SendAudioMessageInput
import com.nabla.sdk.graphql.type.SendDocumentMessageInput
import com.nabla.sdk.graphql.type.SendImageMessageInput
import com.nabla.sdk.graphql.type.SendMessageContentInput
import com.nabla.sdk.graphql.type.SendMessageInput
import com.nabla.sdk.graphql.type.SendTextMessageInput
import com.nabla.sdk.graphql.type.SendVideoMessageInput
import com.nabla.sdk.graphql.type.UploadInput
import com.nabla.sdk.messaging.core.domain.entity.BaseMessage
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.InvalidMessageException
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import kotlinx.datetime.Clock

internal class MessageMapper constructor(
    private val clock: Clock,
    private val uuidGenerator: UuidGenerator,
    private val logger: Logger,
    private val gqlConversationContentDataSource: GqlConversationContentDataSource,
) {

    suspend fun messageInputToNewMessage(
        input: MessageInput,
        conversationId: ConversationId.Remote? = null,
        replyTo: MessageId.Remote? = null,
    ): Message {
        val baseMessage = BaseMessage(
            MessageId.Local(uuidGenerator.generate()),
            clock.now(),
            MessageAuthor.Patient,
            SendStatus.Sending,
            replyTo = if (replyTo != null && conversationId != null) {
                gqlConversationContentDataSource.findMessageInConversationCache(conversationId, replyTo).also { message ->
                    if (message == null) logger.warn("Reply to message not found in cache: $replyTo")
                }
            } else null,
        )
        val message = when (input) {
            is MessageInput.Media.Document -> Message.Media.Document(baseMessage, input.mediaSource)
            is MessageInput.Media.Image -> Message.Media.Image(baseMessage, input.mediaSource)
            is MessageInput.Media.Video -> Message.Media.Video(baseMessage, input.mediaSource)
            is MessageInput.Text -> Message.Text(baseMessage, input.text)
            is MessageInput.Media.Audio -> Message.Media.Audio(baseMessage, input.mediaSource)
        }
        return message
    }

    suspend fun messageToGqlSendMessageInput(
        message: Message,
        fileUploader: suspend Message.Media<*, *>.() -> Uuid,
    ): SendMessageInput {
        return SendMessageInput(
            clientId = message.id.requireLocal().clientId,
            replyToMessageId = Optional.presentIfNotNull(message.replyTo?.id?.remoteId),
            content = when (message) {
                is Message.Media.Audio -> SendMessageContentInput(
                    audioInput = Optional.presentIfNotNull(
                        SendAudioMessageInput(
                            UploadInput(fileUploader(message)),
                        ),
                    ),
                )
                is Message.Media.Document -> SendMessageContentInput(
                    documentInput = Optional.presentIfNotNull(
                        SendDocumentMessageInput(
                            UploadInput(fileUploader(message)),
                        ),
                    ),
                )
                is Message.Media.Image -> SendMessageContentInput(
                    imageInput = Optional.presentIfNotNull(
                        SendImageMessageInput(
                            UploadInput(fileUploader(message)),
                        ),
                    ),
                )
                is Message.Media.Video -> SendMessageContentInput(
                    videoInput = Optional.presentIfNotNull(
                        SendVideoMessageInput(
                            UploadInput(fileUploader(message)),
                        ),
                    ),
                )
                is Message.Text -> SendMessageContentInput(
                    textInput = Optional.presentIfNotNull(SendTextMessageInput(text = message.text)),
                )
                is Message.Deleted -> throw InvalidMessageException("Can't send a deleted message")
            }
        )
    }
}
