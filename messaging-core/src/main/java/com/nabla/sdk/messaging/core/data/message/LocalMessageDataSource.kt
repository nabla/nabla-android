package com.nabla.sdk.messaging.core.data.message

import com.benasher44.uuid.Uuid
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class LocalMessageDataSource {
    private val conversationToLocalMessagesFlows = mutableMapOf<Uuid, MutableStateFlow<Map<Uuid, Message>>>()

    fun watchLocalMessages(conversationId: ConversationId): Flow<Collection<Message>> {
        return getLocalMessagesMutableFlow(conversationId).map { it.values }
    }

    private fun getLocalMessagesMutableFlow(
        conversationId: ConversationId
    ): MutableStateFlow<Map<Uuid, Message>> {
        return synchronized(this) {
            conversationToLocalMessagesFlows.getOrPut(conversationId.stableId) {
                MutableStateFlow(emptyMap())
            }
        }
    }

    fun putMessage(conversationId: ConversationId, message: Message) {
        val stateFlow = getLocalMessagesMutableFlow(conversationId)
        stateFlow.update {
            it + (message.baseMessage.id.stableId to message)
        }
    }

    fun removeMessage(conversationId: ConversationId, localMessageId: MessageId.Local) {
        val stateFlow = getLocalMessagesMutableFlow(conversationId)
        stateFlow.update {
            it - localMessageId.clientId
        }
    }

    fun getMessage(conversationId: ConversationId, localMessageId: MessageId.Local): Message? {
        return getLocalMessagesMutableFlow(conversationId).value[localMessageId.clientId]
    }
}
