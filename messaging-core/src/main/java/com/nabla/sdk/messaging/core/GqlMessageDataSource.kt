package com.nabla.sdk.messaging.core

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.optimisticUpdates
import com.apollographql.apollo3.cache.normalized.watch
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.boundary.FileUploadRepository
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.graphql.DeleteMessageMutation
import com.nabla.sdk.graphql.SendMessageMutation
import com.nabla.sdk.graphql.fragment.DeletedMessageContentFragment
import com.nabla.sdk.graphql.fragment.MessageContentFragment
import com.nabla.sdk.graphql.type.DeleteMessageOutput
import com.nabla.sdk.graphql.type.DeletedMessageContent
import com.nabla.sdk.graphql.type.EmptyObject
import com.nabla.sdk.graphql.type.MessageContent
import com.nabla.sdk.graphql.type.SendDocumentMessageInput
import com.nabla.sdk.graphql.type.SendImageMessageInput
import com.nabla.sdk.graphql.type.SendMessageContentInput
import com.nabla.sdk.graphql.type.SendTextMessageInput
import com.nabla.sdk.graphql.type.UploadInput
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlEventHelper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlHelper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlMapper
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal class GqlMessageDataSource(
    private val apolloClient: ApolloClient,
    private val gqlEventHelper: MessagingGqlEventHelper,
    private val mapper: MessagingGqlMapper,
    private val fileUploadRepository: FileUploadRepository,
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

    suspend fun sendDocumentMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        fileUploadId: Uuid
    ) {
        sendMediaMessage(conversationId, clientId) {
            SendMessageContentInput(
                documentInput = Optional.presentIfNotNull(
                    SendDocumentMessageInput(
                        UploadInput(fileUploadId)
                    )
                ),
            )
        }
    }

    suspend fun sendImageMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        fileUploadId: Uuid
    ) {
        sendMediaMessage(conversationId, clientId) {
            SendMessageContentInput(
                imageInput = Optional.presentIfNotNull(
                    SendImageMessageInput(
                        UploadInput(fileUploadId)
                    )
                ),
            )
        }
    }

    private suspend fun sendMediaMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        inputFactoryBlock: () -> SendMessageContentInput
    ) {
        val input = inputFactoryBlock()
        val mutation = SendMessageMutation(conversationId.value, input, clientId)
        apolloClient.mutation(mutation).execute()
    }

    suspend fun sendTextMessage(conversationId: ConversationId, clientId: Uuid, text: String) {
        val input = SendMessageContentInput(
            textInput = Optional.presentIfNotNull(
                SendTextMessageInput(
                    text = text
                )
            )
        )
        val mutation = SendMessageMutation(conversationId.value, input, clientId)
        apolloClient.mutation(mutation).execute()
    }

    suspend fun deleteMessage(remoteMessageId: Uuid) {
        val mutation = DeleteMessageMutation(remoteMessageId)
        val optimisticData = DeleteMessageMutation.Data(
            deleteMessage = DeleteMessageMutation.DeleteMessage(
                message = DeleteMessageMutation.Message(
                    content = DeleteMessageMutation.Content(
                        __typename = DeleteMessageOutput.type.name, // TODO : Double check expected type with mutation real execution
                        messageContentFragment = MessageContentFragment(
                            __typename = MessageContent.type.name,
                            onTextMessageContent = null,
                            onImageMessageContent = null,
                            onDocumentMessageContent = null,
                            onDeletedMessageContent = MessageContentFragment.OnDeletedMessageContent(
                                __typename = DeletedMessageContent.type.name,
                                deletedMessageContentFragment = DeletedMessageContentFragment(
                                    EmptyObject.EMPTY
                                )
                            )
                        )
                    )
                )
            )
        )

        apolloClient.mutation(mutation)
            .optimisticUpdates(optimisticData)
            .execute()
    }
}
