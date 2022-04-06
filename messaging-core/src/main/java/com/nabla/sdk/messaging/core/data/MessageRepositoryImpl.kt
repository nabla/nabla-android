package com.nabla.sdk.messaging.core.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.watch
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.core.kotlin.sharedSingleIn
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlEventHelper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlHelper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlMapper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlOperationHelper
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MessageRepositoryImpl(
    private val logger: Logger,
    private val repoScope: CoroutineScope,
    private val apolloClient: ApolloClient,
    private val mapper: MessagingGqlMapper,
    private val gqlOperationHelper: MessagingGqlOperationHelper,
    private val gqlEventHelper: MessagingGqlEventHelper,
) : MessageRepository {

    private val loadMoreConversationMessagesSharedSingleLock = Mutex()
    private val loadMoreConversationMessagesSharedSingleMap = mutableMapOf<Id, SharedSingle<Unit>>()

    @OptIn(FlowPreview::class)
    override fun watchConversationMessages(conversationId: Id): Flow<ConversationWithMessages> {
        val query = MessagingGqlHelper.firstMessagePageQuery(conversationId)
        val dataFlow = apolloClient.query(query)
            .watch()
            .map { response -> response.dataAssertNoErrors }
            .map { queryData ->
                val page = queryData.conversation.conversation.conversationMessagesPageFragment.items
                val items = page.data.mapNotNull { it?.messageFragment }.map {
                    mapper.mapToMessage(it)
                }
                return@map ConversationWithMessages(
                    conversation = mapper.mapToConversation(queryData.conversation.conversation.conversationFragment),
                    messages = PaginatedList(items, page.hasMore)
                )
            }
        return flowOf(gqlEventHelper.conversationEventsFlow(conversationId), dataFlow)
            .flattenMerge()
            .filterIsInstance()
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
}
