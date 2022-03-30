package com.nabla.sdk.messaging.core.data.mapper

import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.graphql.fragment.ConversationListItemFragment
import com.nabla.sdk.graphql.fragment.ProviderFragment
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import kotlinx.datetime.Clock

internal class Mapper {
    fun mapToConversation(fragment: ConversationListItemFragment): Conversation {
        return Conversation(
            id = fragment.id,
            inboxPreviewTitle = "",
            inboxPreviewSubtitle = "",
            lastModified = Clock.System.now(),
            patientUnreadMessageCount = fragment.unreadMessageCount,
            providers = fragment.providers.map {
                mapToProvider(it.conversationProviderFragment.provider.providerFragment)
            }
        )
    }

    private fun mapToProvider(fragment: ProviderFragment): User.Provider {
        return User.Provider(
            id = fragment.id,
            avatar = null,
            firstName = "",
            lastName = "",
            title = null,
            prefix = null,
        )
    }
}
