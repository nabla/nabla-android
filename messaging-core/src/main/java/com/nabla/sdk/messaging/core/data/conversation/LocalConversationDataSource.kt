package com.nabla.sdk.messaging.core.data.conversation

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.messaging.core.data.message.LocalConversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class LocalConversationDataSource {
    private val localIdToConversations = MutableStateFlow(emptyMap<ConversationId.Local, LocalConversation>())
    private val remoteIdToLocalId = mutableMapOf<Uuid, Uuid>()

    fun watch(conversationId: ConversationId.Local): Flow<LocalConversation> {
        return localIdToConversations.map { it[conversationId] }.filterNotNull().distinctUntilChanged()
    }

    suspend fun waitConversationCreated(conversationId: ConversationId.Local): ConversationId.Remote {
        return watch(conversationId).map { it.creationState }
            .filterIsInstance<LocalConversation.CreationState.Created>()
            .first()
            .remoteId
    }

    fun create(title: String?, providerIds: List<Uuid>?): ConversationId.Local {
        val conversation = LocalConversation(
            localId = ConversationId.Local(uuid4()),
            creationState = LocalConversation.CreationState.ToBeCreated,
            title = title,
            providerIds = providerIds,
        )
        update(conversation)
        return conversation.localId
    }

    fun update(conversation: LocalConversation) {
        localIdToConversations.update { it + (conversation.localId to conversation) }
        if (conversation.creationState is LocalConversation.CreationState.Created) {
            remoteIdToLocalId[conversation.creationState.remoteId.remoteId] = conversation.localId.clientId
        }
    }

    fun remove(conversationId: ConversationId.Local) {
        localIdToConversations.update { it - conversationId }
    }

    fun findLocalConversationId(remoteId: Uuid): ConversationId {
        val localId = remoteIdToLocalId[remoteId]
        return ConversationId.Remote(
            clientId = localId,
            remoteId = remoteId,
        )
    }
}
