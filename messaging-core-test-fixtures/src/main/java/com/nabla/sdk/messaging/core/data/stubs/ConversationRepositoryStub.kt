package com.nabla.sdk.messaging.core.data.stubs

import androidx.test.espresso.idling.CountingIdlingResource
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.data.stubs.fake
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.RefreshingState
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

internal class ConversationRepositoryStub(private val idlingRes: CountingIdlingResource) : ConversationRepository {
    internal val conversationsFlow = MutableStateFlow(
        PaginatedList(
            items = listOf(
                Conversation.fake(inboxPreviewTitle = "With Unreads", patientUnreadMessageCount = 1),
                Conversation.fake(inboxPreviewTitle = "Without Unreads", patientUnreadMessageCount = 0),
            ) + (2..9).map { Conversation.randomFake() },
            hasMore = true
        )
    )

    val newlyCreatedConversationIds = mutableSetOf<Uuid>()

    override suspend fun createConversation(
        message: MessageInput?,
        title: String?,
        providerIds: List<Uuid>?,
    ): Conversation {
        delayWithIdlingRes(idlingRes, 300.milliseconds)

        val newConversation = Conversation.fake(
            title = "New conversation",
            inboxPreviewTitle = "New conversation",
            providersInConversation = emptyList(),
        )
        newlyCreatedConversationIds.add(newConversation.id.stableId)
        conversationsFlow.update { it.copy(items = listOf(newConversation) + it.items) }

        return newConversation
    }

    override fun createLocalConversation(title: String?, providerIds: List<Uuid>?): ConversationId.Local {
        val newConversation = Conversation.fake(
            id = ConversationId.Remote(uuid4(), uuid4()),
            title = "New conversation",
            inboxPreviewTitle = "New conversation",
            providersInConversation = emptyList(),
        )
        newlyCreatedConversationIds.add(newConversation.id.stableId)
        conversationsFlow.update { it.copy(items = listOf(newConversation) + it.items) }

        return ConversationId.Local(newConversation.id.clientId!!)
    }

    override fun watchConversation(conversationId: ConversationId): Flow<Response<Conversation>> {
        return conversationsFlow.map {
            Response(
                isDataFresh = true,
                refreshingState = RefreshingState.Refreshed,
                data = it.items.first { it.id.stableId == conversationId.stableId },
            )
        }
    }

    override fun watchConversations(): Flow<Response<PaginatedList<Conversation>>> {
        return conversationsFlow
            .onStart {
                delayWithIdlingRes(idlingRes, 100.milliseconds)
                if (MOCK_ERRORS) if (Random.nextBoolean()) throw ApolloNetworkException()
            }
            .map {
                Response(
                    isDataFresh = true,
                    refreshingState = RefreshingState.Refreshed,
                    data = it,
                )
            }
    }

    private fun Conversation.Companion.randomFake() = Conversation.fake(
        lastModified = Clock.System.now().minus((1..1_000).random().minutes),
        providersInConversation = listOf(
            ProviderInConversation.fake(),
            ProviderInConversation.fake(provider = Provider.fake(avatar = null)),
            ProviderInConversation.fake(provider = Provider.fake(avatar = null, lastName = "Doe")),
        ).shuffled().take((0..3).random()),
        patientUnreadMessageCount = (0..3).random(),
    )

    override suspend fun loadMoreConversations() {
        if (!conversationsFlow.value.hasMore) return

        delayWithIdlingRes(idlingRes, 100.milliseconds)
        if (MOCK_ERRORS) if (Random.nextBoolean()) throw ApolloNetworkException()
        val newItems = conversationsFlow.value.items + (0..10).map { Conversation.randomFake() }
        conversationsFlow.value = conversationsFlow.value.copy(
            items = newItems,
            hasMore = newItems.size < 30,
        )
    }

    override suspend fun markConversationAsRead(conversationId: ConversationId) {
        println("markConversationAsRead")
    }

    companion object {
        private const val MOCK_ERRORS = false
    }
}
