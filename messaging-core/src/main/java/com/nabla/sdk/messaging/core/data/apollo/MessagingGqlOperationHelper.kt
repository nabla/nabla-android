package com.nabla.sdk.messaging.core.data.apollo

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.nabla.sdk.core.data.apollo.CacheUpdateOperation
import com.nabla.sdk.core.data.apollo.updateCache
import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.core.domain.entity.toId
import com.nabla.sdk.graphql.ConversationListQuery
import com.nabla.sdk.graphql.fragment.ConversationFragment
import com.nabla.sdk.graphql.fragment.ConversationMessagesPageFragment
import com.nabla.sdk.graphql.fragment.MessageFragment
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlHelper.modify

internal class MessagingGqlOperationHelper constructor(private val apolloClient: ApolloClient) {

    suspend fun insertConversationToConversationsListCache(
        conversation: ConversationFragment,
    ) {
        val query = MessagingGqlHelper.firstConversationsPageQuery()
        apolloClient.updateCache(query) { cachedQueryData ->
            if (cachedQueryData == null) return@updateCache CacheUpdateOperation.Ignore()
            val newItem = ConversationListQuery.Conversation(
                conversation.__typename,
                conversation
            )
            val mergedConversations = listOf(newItem) + cachedQueryData.conversations.conversations
            val mergedQueryData = cachedQueryData.modify(mergedConversations)
            CacheUpdateOperation.Write(mergedQueryData)
        }
    }

    suspend fun insertMessageToConversationCache(
        message: MessageFragment,
    ) {
        val query = MessagingGqlHelper.firstMessagePageQuery(message.id.toId())
        apolloClient.updateCache(query) { cachedQueryData ->
            if (cachedQueryData == null) return@updateCache CacheUpdateOperation.Ignore()
            val newItem = ConversationMessagesPageFragment.Data(
                message.__typename,
                message
            )
            val mergedItemsData = listOf(newItem) + cachedQueryData.conversation.conversation.conversationMessagesPageFragment.items.data
            val mergedQueryData = cachedQueryData.modify(mergedItemsData)
            CacheUpdateOperation.Write(mergedQueryData)
        }
    }

    suspend fun loadMoreConversationMessagesInCache(conversationId: Id) {
        val query = MessagingGqlHelper.firstMessagePageQuery(conversationId)
        apolloClient.updateCache(query) { cachedQueryData ->
            if (cachedQueryData == null || !cachedQueryData.conversation.conversation.conversationMessagesPageFragment.items.hasMore) {
                return@updateCache CacheUpdateOperation.Ignore()
            }
            val nextCursor =
                requireNotNull(cachedQueryData.conversation.conversation.conversationMessagesPageFragment.items.nextCursor)
            val updatedQuery = query.copy(
                pageInfo = OpaqueCursorPage(
                    cursor = Optional.presentIfNotNull(nextCursor.toString()) // TODO : Schema update is probably needed here
                )
            )
            val freshQueryData = apolloClient.query(updatedQuery)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
                .dataAssertNoErrors
            val mergedData =
                (cachedQueryData.conversation.conversation.conversationMessagesPageFragment.items.data + freshQueryData.conversation.conversation.conversationMessagesPageFragment.items.data).distinctBy { it?.messageFragment?.id }
            val mergedQueryData = freshQueryData.modify(mergedData)
            return@updateCache CacheUpdateOperation.Write(mergedQueryData)
        }
    }

    suspend fun loadMoreConversationsInCache() {
        val firstPageQuery = MessagingGqlHelper.firstConversationsPageQuery()
        apolloClient.updateCache(firstPageQuery) { cachedQueryData ->
            if (cachedQueryData == null || !cachedQueryData.conversations.hasMore) {
                return@updateCache CacheUpdateOperation.Ignore()
            }
            val updatedQuery = firstPageQuery.copy(
                pageInfo = OpaqueCursorPage(
                    cursor = Optional.presentIfNotNull(cachedQueryData.conversations.nextCursor)
                )
            )
            val freshQueryData = apolloClient.query(updatedQuery)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
                .dataAssertNoErrors
            val mergedConversations =
                (cachedQueryData.conversations.conversations + freshQueryData.conversations.conversations)
                    .distinctBy { it.conversationFragment.id }
            val mergedQueryData = freshQueryData.modify(mergedConversations)
            CacheUpdateOperation.Write(mergedQueryData)
        }
    }
}
