package com.nabla.sdk.messaging.core.data.conversation

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.RefreshingState
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.core.kotlin.KotlinExt.sharedSingleIn
import com.nabla.sdk.messaging.core.data.message.GqlConversationContentDataSource
import com.nabla.sdk.messaging.core.data.message.LocalConversation
import com.nabla.sdk.messaging.core.data.message.MessageFileUploader
import com.nabla.sdk.messaging.core.data.message.MessageMapper
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

internal class ConversationRepositoryImpl(
    repoScope: CoroutineScope,
    private val localConversationDataSource: LocalConversationDataSource,
    private val gqlConversationDataSource: GqlConversationDataSource,
    private val gqlConversationContentDataSource: GqlConversationContentDataSource,
    private val messageMapper: MessageMapper,
    private val messageFileUploader: MessageFileUploader,
) : ConversationRepository {

    private val loadMoreConversationSharedSingle = sharedSingleIn(repoScope) {
        gqlConversationDataSource.loadMoreConversationsInCache()
    }

    override suspend fun createConversation(
        message: MessageInput?,
        title: String?,
        providerIds: List<Uuid>?,
    ): Conversation {
        val messageInput = message?.let { messageMapper.messageInputToNewMessage(it) }
        val gqlMessageInput = messageInput?.let {
            messageMapper.messageToGqlSendMessageInput(it) {
                messageFileUploader.uploadFile(this)
            }
        }
        return gqlConversationDataSource.createConversation(title, providerIds, gqlMessageInput)
    }

    override fun createLocalConversation(title: String?, providerIds: List<Uuid>?): ConversationId.Local {
        return localConversationDataSource.create(title, providerIds)
    }

    override fun watchConversation(conversationId: ConversationId): Flow<Response<Conversation>> {
        return when (conversationId) {
            is ConversationId.Local -> localConversationDataSource.watch(conversationId).flatMapLatest { localConversation ->
                when (localConversation.creationState) {
                    LocalConversation.CreationState.Creating,
                    is LocalConversation.CreationState.ErrorCreating,
                    LocalConversation.CreationState.ToBeCreated,
                    -> flowOf(
                        Response(
                            isDataFresh = true,
                            refreshingState = RefreshingState.Refreshed,
                            data = localConversation.asConversation(),
                        )
                    )
                    is LocalConversation.CreationState.Created -> {
                        watchRemoteConversation(localConversation.creationState.remoteId)
                    }
                }
            }
            is ConversationId.Remote -> watchRemoteConversation(conversationId)
        }
    }

    @OptIn(FlowPreview::class)
    private fun watchRemoteConversation(
        conversationId: ConversationId.Remote
    ): Flow<Response<Conversation>> {
        return flowOf(
            gqlConversationContentDataSource.conversationEventsFlow(conversationId),
            gqlConversationDataSource.watchConversation(conversationId)
        )
            .flattenMerge()
            .filterIsInstance()
    }

    override fun watchConversations(): Flow<Response<PaginatedList<Conversation>>> {
        return gqlConversationDataSource.watchConversations()
    }

    override suspend fun loadMoreConversations() {
        loadMoreConversationSharedSingle.await()
    }

    override suspend fun markConversationAsRead(conversationId: ConversationId) {
        when (conversationId) {
            is ConversationId.Local -> { /* no-op */ }
            is ConversationId.Remote -> gqlConversationDataSource.markConversationAsRead(conversationId)
        }
    }
}
