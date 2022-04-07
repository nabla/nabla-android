package com.nabla.sdk.messaging.core.domain.entity

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.FileUpload
import kotlinx.datetime.Instant

data class BaseMessage(
    val id: MessageId,
    val sentAt: Instant?,
    val sender: MessageSender,
    val status: MessageStatus,
    val conversationId: ConversationId,
)

sealed class MessageId {
    abstract val stableId: Uuid
    abstract val clientId: Uuid?
    abstract val remoteId: Uuid?

    data class Local(override val clientId: Uuid) : MessageId() {
        override val stableId: Uuid = clientId
        override val remoteId: Uuid? = null
    }
    data class Remote(override val clientId: Uuid?, override val remoteId: Uuid) : MessageId() {
        override val stableId: Uuid = clientId ?: remoteId
    }
}

sealed class Message {
    abstract val message: BaseMessage
    abstract fun modify(status: MessageStatus = message.status): Message

    data class Text(override val message: BaseMessage, val text: String) : Message() {
        override fun modify(status: MessageStatus): Message {
            return copy(message = message.copy(status = status))
        }
        companion object
    }

    sealed class Media : Message() {

        data class Image(
            override val message: BaseMessage,
            val image: FileUpload.Image,
        ) : Media() {
            override fun modify(status: MessageStatus): Message {
                return copy(message = message.copy(status = status))
            }
            companion object
        }

        data class Document(
            override val message: BaseMessage,
            val document: FileUpload.Document
        ) : Media() {
            override fun modify(status: MessageStatus): Message {
                return copy(message = message.copy(status = status))
            }
            companion object
        }
    }

    data class Deleted(override val message: BaseMessage) : Message() {
        override fun modify(status: MessageStatus): Message {
            return copy(message = message.copy(status = status))
        }
        companion object
    }
}
