package com.nabla.sdk.messaging.core.data.mapper

import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.graphql.fragment.ConversationFragment
import com.nabla.sdk.graphql.fragment.ProviderInConversationFragment
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import kotlinx.datetime.Clock

internal class Mapper {
    fun mapToConversation(fragment: ConversationFragment): Conversation {
        return Conversation(
            id = Id(fragment.id),
            inboxPreviewTitle = "",
            inboxPreviewSubtitle = "",
            lastModified = Clock.System.now(),
            patientUnreadMessageCount = fragment.unreadMessageCount,
            providersInConversation = fragment.providers.map {
                mapToProviderInConversation(it.providerInConversationFragment)
            }
        )
    }

    private fun mapToProviderInConversation(
        fragment: ProviderInConversationFragment
    ): ProviderInConversation {
        return ProviderInConversation(
            provider = User.Provider(
                id = Id(fragment.id),
                avatar = null,
                firstName = "",
                lastName = "",
                title = null,
                prefix = null,
            ),
            isTyping = fragment.isTyping,
            seenUntil = Clock.System.now(), // TODO we need Date custom-scalar config
        )
    }
}
