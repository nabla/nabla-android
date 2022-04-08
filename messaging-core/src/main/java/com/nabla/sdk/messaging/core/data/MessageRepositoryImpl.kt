package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.core.kotlin.sharedSingleIn
import com.nabla.sdk.messaging.core.GqlMessageDataSource
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlOperationHelper
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
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
    private val loadMoreConversationMessagesSharedSingleMap = mutableMapOf<ConversationId, SharedSingle<Unit>>()

    override fun watchConversationMessages(conversationId: ConversationId): Flow<ConversationWithMessages> {
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

    override suspend fun loadMoreMessages(conversationId: ConversationId) {
        val loadMoreConversationMessagesSharedSingle = loadMoreConversationMessagesSharedSingleLock.withLock {
            loadMoreConversationMessagesSharedSingleMap.getOrPut(conversationId) {
                sharedSingleIn(repoScope) {
                    gqlOperationHelper.loadMoreConversationMessagesInCache(conversationId)
                }
            }
        }
        loadMoreConversationMessagesSharedSingle.await()
    }

    override suspend fun sendMessage(message: Message) {
        localMessageDataSource.putMessage(message)
        runCatchingCancellable {
            gqlMessageDataSource.sendMessage(message)
        }.onFailure {
            localMessageDataSource.putMessage(
                message.modify(MessageStatus.ErrorSending)
            )
        }
    }

    override suspend fun retrySendingMessage(conversationId: ConversationId, localMessageId: MessageId.Local) {
        TODO("Not yet implemented")
    }

    override suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMessage(conversationId: ConversationId, messsageId: MessageId) {
        TODO("Not yet implemented")
    }
}
