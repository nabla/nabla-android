package com.nabla.sdk.messaging.core.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.nabla.sdk.core.domain.entity.PaginatedResult
import com.nabla.sdk.graphql.ConversationListQuery
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.messaging.core.data.mapper.Mapper
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import kotlinx.coroutines.flow.Flow

internal class ConversationRepositoryImpl constructor(
    private val apolloClient: ApolloClient,
    private val mapper: Mapper,
) : ConversationRepository {

    override suspend fun createConversation() {
        TODO("Not yet implemented")
    }

    override suspend fun getConversationsPage(
        cursor: String?
    ): PaginatedResult<Conversation> {
        val query = ConversationListQuery(OpaqueCursorPage(cursor = Optional.Present(cursor)))
        val response = apolloClient.query(query).execute()
        return response.dataAssertNoErrors.conversations.let {
            val items = it.conversations.map { mapper.mapToConversation(it.conversationListItemFragment) }
            PaginatedResult(
                items = items,
                nextCursor = it.nextCursor
            )
        }
    }

    override fun watchConversations(): Flow<List<Conversation>> {
        TODO("Not yet implemented")
    }
}
