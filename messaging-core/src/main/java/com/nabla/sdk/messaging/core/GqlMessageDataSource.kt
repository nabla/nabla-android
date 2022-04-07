package com.nabla.sdk.messaging.core

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.watch
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.graphql.SendMessageMutation
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlEventHelper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlHelper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlMapper
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal class GqlMessageDataSource(
    private val apolloClient: ApolloClient,
    private val gqlEventHelper: MessagingGqlEventHelper,
    private val mapper: MessagingGqlMapper,
) {

    fun watchRemoteConversationWithMessages(conversationId: ConversationId): Flow<ConversationWithMessages> {
        val query = MessagingGqlHelper.firstMessagePageQuery(conversationId)
        val dataFlow = apolloClient.query(query)
            .watch()
            .map { response -> response.dataAssertNoErrors }
            .map { queryData ->
                val page =
                    queryData.conversation.conversation.conversationMessagesPageFragment.items
                val items = page.data.mapNotNull { it?.messageFragment }.map {
                    mapper.mapToMessage(it)
                }
                return@map ConversationWithMessages(
                    conversation = mapper.mapToConversation(queryData.conversation.conversation.conversationFragment),
                    messages = PaginatedList(items, page.hasMore)
                )
            }
        return flowOf(
            gqlEventHelper.conversationEventsFlow(conversationId),
            dataFlow
        ).flattenMerge().filterIsInstance()
    }

    suspend fun sendMessage(message: Message) {
        val input = mapper.mapToSendMessageContentInput(message)
        val mutation = SendMessageMutation(
            message.message.conversationId.value,
            input,
            requireNotNull(message.message.id.clientId)
        )
        apolloClient.mutation(mutation).execute()
    }
}
