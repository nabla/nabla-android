package com.nabla.sdk.messaging.core.data.apollo

import com.apollographql.apollo3.ApolloClient
import com.nabla.sdk.graphql.ConversationEventsSubscription
import com.nabla.sdk.graphql.ConversationsEventsSubscription
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn

internal class MessagingGqlEventHelper constructor(
    private val apolloClient: ApolloClient,
    private val coroutineScope: CoroutineScope,
    private val gqlOperationHelper: MessagingGqlOperationHelper,
) {

    val conversationsEventsFlow = apolloClient.subscription(ConversationsEventsSubscription())
        .toFlow()
        .map {
            it.dataAssertNoErrors
        }.onEach {
            it.conversations.onConversationCreatedEvent?.conversation?.conversationFragment?.let {
                gqlOperationHelper.insertConversationToConversationsListCache(it)
            }
        }.shareIn(
            scope = coroutineScope,
            replay = 0,
            started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0)
        )
    private val conversationEventsFlowMap = mutableMapOf<ConversationId, Flow<Unit>>()

    fun conversationEventsFlow(conversationId: ConversationId): Flow<Unit> {
        return synchronized(this) {
            return@synchronized conversationEventsFlowMap.getOrPut(conversationId) {
                createConversationEventsFlow(conversationId)
            }
        }
    }

    private fun createConversationEventsFlow(conversationId: ConversationId): Flow<Unit> {
        return apolloClient.subscription(ConversationEventsSubscription(conversationId.value))
            .toFlow()
            .map { it.dataAssertNoErrors }
            .onEach {
                it.conversation.onMessageCreatedEvent?.message?.messageFragment?.let {
                    gqlOperationHelper.insertMessageToConversationCache(it)
                }
            }.shareIn(
                scope = coroutineScope,
                replay = 0,
                started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0)
            ).filterIsInstance()
    }
}
