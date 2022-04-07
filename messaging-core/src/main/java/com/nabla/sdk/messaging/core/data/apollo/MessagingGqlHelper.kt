package com.nabla.sdk.messaging.core.data.apollo

import com.apollographql.apollo3.api.Optional
import com.nabla.sdk.graphql.ConversationListQuery
import com.nabla.sdk.graphql.ConversationQuery
import com.nabla.sdk.graphql.fragment.ConversationMessagesPageFragment
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.messaging.core.domain.entity.ConversationId

internal object MessagingGqlHelper {

    fun firstConversationsPageQuery() =
        ConversationListQuery(OpaqueCursorPage(cursor = Optional.Absent))

    fun firstMessagePageQuery(id: ConversationId) =
        ConversationQuery(id.value, OpaqueCursorPage(cursor = Optional.Absent))

    fun ConversationListQuery.Data.modify(
        conversations: List<ConversationListQuery.Conversation> = this.conversations.conversations
    ): ConversationListQuery.Data {
        return copy(
            conversations = this.conversations.copy(
                conversations = conversations
            )
        )
    }

    fun ConversationQuery.Data.modify(
        data: List<ConversationMessagesPageFragment.Data?> = conversation.conversation.conversationMessagesPageFragment.items.data
    ): ConversationQuery.Data {
        return copy(
            conversation = conversation.copy(
                conversation = conversation.conversation.copy(
                    conversationMessagesPageFragment = conversation.conversation.conversationMessagesPageFragment.copy(
                        items = conversation.conversation.conversationMessagesPageFragment.items.copy(
                            data = data
                        )
                    )
                )
            )
        )
    }
}
