package com.nabla.sdk.messaging.core.data.stubs

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.graphql.ConversationEventsSubscription
import com.nabla.sdk.messaging.graphql.ConversationItemsQuery
import com.nabla.sdk.messaging.graphql.ConversationsEventsSubscription
import com.nabla.sdk.messaging.graphql.ConversationsQuery
import com.nabla.sdk.messaging.graphql.test.ConversationEventsSubscription_TestBuilder.Data
import com.nabla.sdk.messaging.graphql.test.ConversationItemsQuery_TestBuilder
import com.nabla.sdk.messaging.graphql.test.ConversationItemsQuery_TestBuilder.Data
import com.nabla.sdk.messaging.graphql.test.ConversationsEventsSubscription_TestBuilder.Data
import com.nabla.sdk.messaging.graphql.test.ConversationsQuery_TestBuilder
import com.nabla.sdk.messaging.graphql.test.ConversationsQuery_TestBuilder.Data
import com.nabla.sdk.tests.common.apollo.CustomTestResolver
import kotlinx.datetime.Instant

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
                        id = conversationId.remoteId.toString()
                        items = items {
                            data = emptyList()
                        }
                    }
                }
            }

        fun single(
            conversationId: ConversationId,
            block: ConversationItemsQuery_TestBuilder.ItemsBuilder.() -> Unit = {},
        ) = ConversationItemsQuery.Data(CustomTestResolver()) {
            conversation = conversation {
                conversation = conversation {
                    id = conversationId.remoteId.toString()
                    items = items {
                        data = listOf(
                            messageData {
                                messageContent = textMessageContentMessageContent { }
                                replyTo = null
                            }
                        )
                        block()
                    }
                }
            }
        }
    }

    object ConversationsEvents {
        fun conversationUpdatedForPatientUnreadMessageCount(
            conversationId: ConversationId,
            patientUnreadMessageCount: Int = 0,
        ) = ConversationsEventsSubscription.Data(CustomTestResolver()) {
            conversations = conversations {
                event = conversationUpdatedEventEvent {
                    conversation = conversation {
                        id = conversationId.remoteId.toString()
                        unreadMessageCount = patientUnreadMessageCount
                        providers = emptyList() // Empty providers list to avoid generating typing events
                    }
                }
            }
        }

        fun conversationCreated(conversationId: ConversationId? = null) = ConversationsEventsSubscription.Data(
            CustomTestResolver()
        ) {
            conversations = conversations {
                event = conversationCreatedEventEvent {
                    conversation = conversation {
                        conversationId?.let { id = conversationId.remoteId.toString() }
                    }
                }
            }
        }

        fun providerJoinsConversation(
            conversationId: ConversationId,
            providerInConversationId: Uuid = uuid4(),
            providerId: Uuid = uuid4(),
            providerIsTypingAt: Instant? = null,
        ) = ConversationsEventsSubscription.Data(CustomTestResolver()) {
            conversations = conversations {
                event = conversationUpdatedEventEvent {
                    conversation = conversation {
                        id = conversationId.remoteId.toString()
                        providers = listOf(
                            provider {
                                id = providerInConversationId.toString()
                                typingAt = providerIsTypingAt?.toString()
                                provider = provider {
                                    id = providerId.toString()
                                }
                            }
                        )
                    }
                }
            }
        }

        fun providersLeaveConversation(
            conversationId: ConversationId,
        ) = ConversationsEventsSubscription.Data(CustomTestResolver()) {
            conversations = conversations {
                event = conversationUpdatedEventEvent {
                    conversation = conversation {
                        id = conversationId.remoteId.toString()
                        providers = listOf()
                    }
                }
            }
        }
    }

    object ConversationEvents {
        object Typing {
            fun providerIsTyping(
                providerInConversationId: Uuid,
                providerId: Uuid,
                typingAtInstant: Instant?,
            ) = ConversationEventsSubscription.Data(CustomTestResolver()) {
                conversation = conversation {
                    event = typingEventEvent {
                        provider = provider {
                            id = providerInConversationId.toString()
                            provider = provider {
                                id = providerId.toString()
                            }
                            typingAt = typingAtInstant?.toString()
                        }
                    }
                }
            }
        }

        object MessageDeleted {
            fun deletedPatientMessage(
                conversationId: ConversationId,
                messageId: MessageId,
            ) = ConversationEventsSubscription.Data(CustomTestResolver()) {
                conversation = conversation {
                    event = messageUpdatedEventEvent {
                        message = message {
                            id = messageId.remoteId.toString()
                            clientId = messageId.clientId.toString()
                            messageContent = deletedMessageContentMessageContent { }
                            conversation = conversation {
                                id = conversationId.remoteId.toString()
                            }
                            author = patientAuthor {}
                            replyTo = null
                        }
                    }
                }
            }
        }

        object Activity {
            fun existingProviderJoinedActivity(
                conversationId: ConversationId,
                providerId: Uuid = uuid4(),
            ) = ConversationEventsSubscription.Data(CustomTestResolver()) {
                conversation = conversation {
                    event = conversationActivityCreatedEvent {
                        activity = activity {
                            conversation = conversation {
                                id = conversationId.remoteId.toString()
                            }
                            conversationActivityContent = providerJoinedConversationConversationActivityContent {
                                provider = providerProvider {
                                    id = providerId.toString()
                                }
                            }
                        }
                    }
                }
            }

            fun deletedProviderJoinedActivity(
                conversationId: ConversationId,
            ) = ConversationEventsSubscription.Data(CustomTestResolver()) {
                conversation = conversation {
                    event = conversationActivityCreatedEvent {
                        activity = activity {
                            conversation = conversation {
                                id = conversationId.remoteId.toString()
                            }
                            conversationActivityContent = providerJoinedConversationConversationActivityContent {
                                provider = deletedProviderProvider { }
                            }
                        }
                    }
                }
            }
        }

        object MessageCreated {
            fun patientTextMessage(
                conversationId: ConversationId,
                localMessageId: MessageId = MessageId.Local(uuid4()),
            ) = ConversationEventsSubscription.Data(CustomTestResolver()) {
                conversation = conversation {
                    event = messageCreatedEventEvent {
                        message = message {
                            clientId = localMessageId.clientId.toString()
                            messageContent = textMessageContentMessageContent { }
                            conversation = conversation {
                                id = conversationId.remoteId.toString()
                            }
                            author = patientAuthor {}
                            replyTo = null
                        }
                    }
                }
            }

            fun patientImageMessage(
                conversationId: ConversationId,
                localMessageId: MessageId = MessageId.Local(uuid4()),
            ) = ConversationEventsSubscription.Data(CustomTestResolver()) {
                conversation = conversation {
                    event = messageCreatedEventEvent {
                        message = message {
                            clientId = localMessageId.clientId.toString()
                            messageContent = imageMessageContentMessageContent {
                                imageFileUpload = imageFileUpload {
                                    mimeType = MimeType.Image.Jpeg.stringRepresentation
                                }
                            }
                            conversation = conversation {
                                id = conversationId.remoteId.toString()
                            }
                            author = patientAuthor {}
                            replyTo = null
                        }
                    }
                }
            }

            fun patientDocumentMessage(
                conversationId: ConversationId,
                localMessageId: MessageId = MessageId.Local(uuid4()),
            ) = ConversationEventsSubscription.Data(CustomTestResolver()) {
                conversation = conversation {
                    event = messageCreatedEventEvent {
                        message = message {
                            clientId = localMessageId.clientId.toString()
                            messageContent = documentMessageContentMessageContent {
                                documentFileUpload = documentFileUpload {
                                    mimeType = MimeType.Application.Pdf.stringRepresentation
                                    thumbnail = thumbnail {
                                        mimeType = MimeType.Image.Jpeg.stringRepresentation
                                    }
                                }
                            }
                            conversation = conversation {
                                id = conversationId.remoteId.toString()
                            }
                            author = patientAuthor {}
                            replyTo = null
                        }
                    }
                }
            }
        }
    }
}
