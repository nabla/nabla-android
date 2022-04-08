package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.core.domain.entity.fake
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ConversationRepositoryMock : ConversationRepository {
    override suspend fun createConversation() {
        // Stub
        println("createConversation")
    }

    override fun watchConversations(): Flow<PaginatedList<Conversation>> {
        return flow {
            delay(1.seconds)
            emit(
                PaginatedList(
                    items = listOf(
                        Conversation.fake(
                            lastModified = Clock.System.now().minus(20.minutes),
                            providersInConversation = listOf(ProviderInConversation.fake(provider = User.Provider.fake(avatar = null)))
                        ),
                        Conversation.fake(
                            lastModified = Clock.System.now().minus(1.days),
                            providersInConversation = emptyList(),
                        ),
                        Conversation.fake(
                            lastModified = Clock.System.now().minus(20.days),
                            patientUnreadMessageCount = 3,
                        ),
                    ),
                    hasMore = true
                )
            )
        }
    }

    override suspend fun loadMoreConversations() {
        println("loadMoreConversations")
    }

    override fun markConversationAsRead(conversationId: ConversationId) {
        println("markConversationAsRead")
    }
}
