package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.core.kotlin.sharedSingleIn
import com.nabla.sdk.messaging.core.GqlMessageDataSource
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlOperationHelper
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MessageRepositoryImpl(
    private val repoScope: CoroutineScope,
    private val gqlOperationHelper: MessagingGqlOperationHelper,
    private val localMessageDataSource: LocalMessageDataSource,
    private val gqlMessageDataSource: GqlMessageDataSource,
) : MessageRepository {

    private val loadMoreConversationMessagesSharedSingleLock = Mutex()
    private val loadMoreConversationMessagesSharedSingleMap = mutableMapOf<Id, SharedSingle<Unit>>()

    override fun watchConversationMessages(conversationId: Id): Flow<ConversationWithMessages> {
        val gqlConversationAndMessagesFlow = gqlMessageDataSource.watchRemoteConversationWithMessages(conversationId)
        val localMessagesFlow = localMessageDataSource.watchLocalMessages(conversationId)
        val messageFlow = gqlConversationAndMessagesFlow.combine(localMessagesFlow) { gqlConversationAndMessages, localMessages ->
            return@combine combineGqlAndLocalInfo(localMessages, gqlConversationAndMessages)
        }
        return messageFlow
    }

    private fun combineGqlAndLocalInfo(
        localMessages: Collection<Message>,
        gqlConversationAndMessages: ConversationWithMessages
    ): ConversationWithMessages {
        val localMessagesToAdd = localMessages.filter { localMessage ->
            localMessage.message.id.stableId !in gqlConversationAndMessages.messages.items.map { it.message.id.stableId }
        }
        val mergedMessages =
            (gqlConversationAndMessages.messages.items + localMessagesToAdd).sortedBy { it.message.sentAt }
        return gqlConversationAndMessages.copy(
            messages = gqlConversationAndMessages.messages.copy(
                items = mergedMessages
            )
        )
    }

    override suspend fun loadMoreMessages(conversationId: Id) {
        val loadMoreConversationMessagesSharedSingle = loadMoreConversationMessagesSharedSingleLock.withLock {
            loadMoreConversationMessagesSharedSingleMap.getOrPut(conversationId) {
                sharedSingleIn(repoScope) {
                    gqlOperationHelper.loadMoreConversationMessagesInCache(conversationId)
                }
            }
        }
        loadMoreConversationMessagesSharedSingle.await()
    }

    override suspend fun sendMessage(conversationId: Id, message: Message) {
        localMessageDataSource.putMessage(conversationId, message)
        runCatchingCancellable {
            gqlMessageDataSource.sendMessage(conversationId, message)
        }.onFailure {
            localMessageDataSource.putMessage(
                conversationId,
                message.modify(MessageStatus.ErrorSending)
            )
        }
    }
}
