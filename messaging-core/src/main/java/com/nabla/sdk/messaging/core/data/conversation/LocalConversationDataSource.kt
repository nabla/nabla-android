package com.nabla.sdk.messaging.core.data.conversation

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.messaging.core.data.message.LocalConversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class LocalConversationDataSource {
    private val conversations = MutableStateFlow(emptyMap<Uuid, LocalConversation>())

    fun watch(conversationId: ConversationId.Local): Flow<LocalConversation> {
        return conversations.map { it[conversationId.clientId] }.filterNotNull()
    }

    suspend fun waitConversationCreated(conversationId: ConversationId.Local): ConversationId.Remote {
        return watch(conversationId).map { it.creationState }
            .filterIsInstance<LocalConversation.CreationState.Created>()
            .first().remoteId
    }

    fun create(title: String?, providerIds: List<Uuid>?): ConversationId.Local {
        val conversation = LocalConversation(
            localId = ConversationId.Local(uuid4()),
            creationState = LocalConversation.CreationState.ToBeCreated,
            title = title,
            providerIds = providerIds
        )
        update(conversation)
        return conversation.localId
    }

    fun update(conversation: LocalConversation) {
        conversations.update { it + (conversation.localId.clientId to conversation) }
    }

    fun remove(conversationId: ConversationId.Local) {
        conversations.update { it - conversationId.clientId }
    }
}
