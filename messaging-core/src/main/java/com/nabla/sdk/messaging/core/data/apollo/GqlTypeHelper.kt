package com.nabla.sdk.messaging.core.data.apollo

import com.nabla.sdk.messaging.graphql.ConversationItemsQuery
import com.nabla.sdk.messaging.graphql.ConversationsQuery
import com.nabla.sdk.messaging.graphql.fragment.ConversationItemsPageFragment

internal object GqlTypeHelper {

    fun ConversationsQuery.Data.modify(
        conversations: List<ConversationsQuery.Conversation> = this.conversations.conversations,
    ): ConversationsQuery.Data {
        return copy(
            conversations = this.conversations.copy(
                conversations = conversations,
            ),
        )
    }

    fun ConversationItemsQuery.Data.modify(
        data: List<ConversationItemsPageFragment.Data?> = conversation.conversation.conversationItemsPageFragment.items.data,
    ): ConversationItemsQuery.Data {
        return copy(
            conversation = conversation.copy(
                conversation = conversation.conversation.copy(
                    conversationItemsPageFragment = conversation.conversation.conversationItemsPageFragment.copy(
                        items = conversation.conversation.conversationItemsPageFragment.items.copy(
                            data = data,
                        ),
                    ),
                ),
            ),
        )
    }
}
