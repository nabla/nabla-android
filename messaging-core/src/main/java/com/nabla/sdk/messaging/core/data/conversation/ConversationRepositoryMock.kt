package com.nabla.sdk.messaging.core.data.conversation

import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.core.domain.entity.fake
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class ConversationRepositoryMock : ConversationRepository {
    private val conversationsFlow = MutableStateFlow(
        PaginatedList(
            items = (0..10).map { Conversation.randomFake() },
            hasMore = true
        )
    )

    override suspend fun createConversation(): Conversation {
        return Conversation.randomFake()
    }

    override fun watchConversations(): Flow<PaginatedList<Conversation>> {
        return conversationsFlow
            .onStart {
                delay(1.seconds)
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

        delay(1.seconds)
        val newItems = conversationsFlow.value.items + (0..10).map { Conversation.randomFake() }
        conversationsFlow.value = conversationsFlow.value.copy(
            items = newItems,
            hasMore = newItems.size < 30,
        )
    }

    override suspend fun markConversationAsRead(conversationId: ConversationId) {
        println("markConversationAsRead")
    }
}
