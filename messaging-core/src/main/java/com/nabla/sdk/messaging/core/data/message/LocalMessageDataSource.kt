package com.nabla.sdk.messaging.core.data.message

import com.benasher44.uuid.Uuid
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class LocalMessageDataSource {
    private val conversationToLocalMessagesFlows = mutableMapOf<ConversationId, MutableStateFlow<Map<Uuid, Message>>>()

    fun watchLocalMessages(conversationId: ConversationId): Flow<Collection<Message>> {
        return getLocalMessagesMutableFlow(conversationId).map { it.values }
    }

    private fun getLocalMessagesMutableFlow(
        conversationId: ConversationId
    ): MutableStateFlow<Map<Uuid, Message>> {
        return synchronized(this) {
            conversationToLocalMessagesFlows.getOrPut(conversationId) {
                MutableStateFlow(emptyMap())
            }
        }
    }

    fun putMessage(message: Message) {
        val stateFlow = getLocalMessagesMutableFlow(message.baseMessage.conversationId)
        stateFlow.value = stateFlow.value.toMutableMap().apply {
            put(message.baseMessage.id.stableId, message)
        }
    }

    fun remove(conversationId: ConversationId, messageClientId: Uuid) {
        val stateFlow = getLocalMessagesMutableFlow(conversationId)
        stateFlow.value = stateFlow.value.toMutableMap().apply {
            remove(messageClientId)
        }
    }
}
