package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.messaging.core.domain.entity.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class LocalMessageDataSource {
    private val conversationToLocalMessagesFlows = mutableMapOf<Id, MutableStateFlow<Map<Id, Message>>>()

    fun watchLocalMessages(conversationId: Id): Flow<Collection<Message>> {
        return getLocalMessagesMutableFlow(conversationId).map { it.values }
    }

    private fun getLocalMessagesMutableFlow(
        conversationId: Id
    ): MutableStateFlow<Map<Id, Message>> {
        return synchronized(this) {
            conversationToLocalMessagesFlows.getOrPut(conversationId) {
                MutableStateFlow(emptyMap())
            }
        }
    }

    fun putMessage(conversationId: Id, message: Message) {
        val stateFlow = getLocalMessagesMutableFlow(conversationId)
        stateFlow.value = stateFlow.value.toMutableMap().apply {
            put(message.message.id.stableId, message)
        }
    }

    fun remove(conversationId: Id, messageId: Id) {
        val stateFlow = getLocalMessagesMutableFlow(conversationId)
        stateFlow.value = stateFlow.value.toMutableMap().apply {
            remove(messageId)
        }
    }
}
