package com.nabla.sdk.messaging.core.data.stubs

import androidx.test.espresso.idling.CountingIdlingResource
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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

    val newlyCreatedConversationIds = mutableSetOf<ConversationId>()

    override suspend fun createConversation(): Conversation {
        delayWithIdlingRes(idlingRes, 300.milliseconds)

        val newConversation = Conversation.fake(
            title = "New conversation",
            inboxPreviewTitle = "New conversation",
            providersInConversation = emptyList(),
        )
        newlyCreatedConversationIds.add(newConversation.id)
        conversationsFlow.value = conversationsFlow.value.copy(items = listOf(newConversation) + conversationsFlow.value.items)

        return newConversation
    }

    override fun watchConversation(conversationId: ConversationId): Flow<Conversation> {
        return conversationsFlow.map { it.items.first { it.id == conversationId } }
    }

    override fun watchConversations(): Flow<PaginatedList<Conversation>> {
        return conversationsFlow
            .onStart {
                delayWithIdlingRes(idlingRes, 100.milliseconds)
                if (MOCK_ERRORS) if (Random.nextBoolean()) throw ApolloNetworkException()
            }
    }

    private fun Conversation.Companion.randomFake() = Conversation.fake(
        lastModified = Clock.System.now().minus((1..1_000).random().minutes),
        providersInConversation = listOf(
            ProviderInConversation.fake(),
            ProviderInConversation.fake(provider = User.Provider.fake(avatar = null)),
            ProviderInConversation.fake(provider = User.Provider.fake(avatar = null, lastName = "Doe")),
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
