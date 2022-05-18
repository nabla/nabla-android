package com.nabla.sdk.messaging.core.data.apollo

import com.nabla.sdk.graphql.ConversationListQuery
import com.nabla.sdk.graphql.ConversationMessagesQuery
import com.nabla.sdk.graphql.fragment.ConversationItemsPageFragment

internal object GqlTypeHelper {

    fun ConversationListQuery.Data.modify(
        conversations: List<ConversationListQuery.Conversation> = this.conversations.conversations
    ): ConversationListQuery.Data {
        return copy(
            conversations = this.conversations.copy(
                conversations = conversations
            )
        )
    }

    fun ConversationMessagesQuery.Data.modify(
        data: List<ConversationItemsPageFragment.Data?> = conversation.conversation.conversationItemsPageFragment.items.data
    ): ConversationMessagesQuery.Data {
        return copy(
            conversation = conversation.copy(
                conversation = conversation.conversation.copy(
                    conversationItemsPageFragment = conversation.conversation.conversationItemsPageFragment.copy(
                        items = conversation.conversation.conversationItemsPageFragment.items.copy(
                            data = data
                        )
                    )
                )
            )
        )
    }
}
