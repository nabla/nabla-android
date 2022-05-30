package com.nabla.sdk.messaging.core.data.stubs

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.nabla.sdk.graphql.ConversationEventsSubscription
import com.nabla.sdk.graphql.ConversationItemsQuery
import com.nabla.sdk.graphql.ConversationsEventsSubscription
import com.nabla.sdk.graphql.ConversationsQuery
import com.nabla.sdk.graphql.test.ConversationEventsSubscription_TestBuilder.Data
import com.nabla.sdk.graphql.test.ConversationItemsQuery_TestBuilder
import com.nabla.sdk.graphql.test.ConversationItemsQuery_TestBuilder.Data
import com.nabla.sdk.graphql.test.ConversationsEventsSubscription_TestBuilder.Data
import com.nabla.sdk.graphql.test.ConversationsQuery_TestBuilder
import com.nabla.sdk.graphql.test.ConversationsQuery_TestBuilder.Data
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.test.apollo.CustomTestResolver

@OptIn(ApolloExperimental::class)
internal object GqlData {
    object Conversations {
        fun empty() = ConversationsQuery.Data {
            conversations = conversations {
                conversations = emptyList()
            }
        }

        fun single(block: ConversationsQuery_TestBuilder.ConversationsBuilder.() -> Unit = {}) =
            ConversationsQuery.Data(CustomTestResolver()) {
                conversations = conversations {
                    conversations = listOf(conversation { })
                    block()
                }
            }
    }

    object ConversationItems {
        fun empty(conversationId: ConversationId) =
            ConversationItemsQuery.Data(CustomTestResolver()) {
                conversation = conversation {
                    conversation = conversation {
                        id = conversationId.value.toString()
                        items = items {
                            data = emptyList()
                        }
                    }
                }
            }

        fun single(
            conversationId: ConversationId,
            block: ConversationItemsQuery_TestBuilder.ItemsBuilder.() -> Unit = {}
        ) = ConversationItemsQuery.Data(CustomTestResolver()) {
            conversation = conversation {
                conversation = conversation {
                    id = conversationId.value.toString()
                    items = items {
                        data = listOf(
                            messageData {
                                messageContent = textMessageContentMessageContent { }
                            }
                        )
                        block()
                    }
                }
            }
        }
    }

    object ConversationsEvents {
        fun conversationCreated(conversationId: ConversationId? = null) = ConversationsEventsSubscription.Data(CustomTestResolver()) {
            conversations = conversations {
                event = conversationCreatedEventEvent {
                    conversation = conversation {
                        conversationId?.let { id = conversationId.value.toString() }
                    }
                }
            }
        }
    }

    object ConversationEvents {
        object MessageCreated {
            fun textMessage(conversationId: ConversationId) =
                ConversationEventsSubscription.Data(CustomTestResolver()) {
                    conversation = conversation {
                        event = messageCreatedEventEvent {
                            message = message {
                                messageContent = textMessageContentMessageContent { }
                                conversation = conversation {
                                    id = conversationId.value.toString()
                                }
                            }
                        }
                    }
                }
        }
    }
}
