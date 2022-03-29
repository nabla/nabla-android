package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.core.domain.entity.Attachment
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class ConversationRepositoryImpl() : ConversationRepository {
    override suspend fun createConversation() {
        // Stub
    }

    override fun watchConversations(): Flow<List<Conversation>> {
        return flow {
            delay(1.seconds)
            emit(
                listOf(
                    Conversation(
                        "id1",
                        "title 1",
                        "subtitle 1",
                        lastModified = Clock.System.now().minus(20.minutes),
                        patientUnreadMessageCount = 0,
                        providers = listOf(
                            User.Provider(
                                "id_provider1",
                                avatar = null,
                                "Véronique",
                                "Cayol",
                                "Gynécologue",
                                "Dr",
                            )
                        )
                    ),
                    Conversation(
                        "id2",
                        "title 2",
                        "subtitle 2",
                        lastModified = Clock.System.now().minus(1.days),
                        patientUnreadMessageCount = 0,
                        providers = emptyList()
                    ),
                    Conversation(
                        "id3",
                        "title 3",
                        "subtitle 3",
                        lastModified = Clock.System.now().minus(20.days),
                        patientUnreadMessageCount = 3,
                        providers = listOf(
                            User.Provider(
                                "id_provider2",
                                avatar = Attachment(
                                    "attachement1",
                                    url = "https://i.pravatar.cc/300",
                                    mimeType = MimeType.Generic("image/png"),
                                    thumbnailUrl = "",
                                ),
                                "John",
                                "Doe",
                                "Généraliste",
                                "Dr",
                            ),
                        )
                    ),
                )
            )
        }
    }
}
