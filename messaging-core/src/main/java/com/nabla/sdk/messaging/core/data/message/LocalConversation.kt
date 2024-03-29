package com.nabla.sdk.messaging.core.data.message

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
import com.nabla.sdk.core.domain.entity.StringOrRes.Companion.asStringOrRes
import com.nabla.sdk.messaging.core.R
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.datetime.Clock

internal data class LocalConversation(
    val localId: ConversationId.Local,
    val creationState: CreationState,
    val title: String?,
    val providerIds: List<Uuid>?,
) {
    fun asConversation(): Conversation {
        return Conversation(
            id = localId,
            title = title,
            subtitle = null,
            inboxPreviewTitle = R.string.nabla_draft_conversation_title_placeholder.asStringOrRes(),
            lastMessagePreview = null,
            lastMessage = null,
            lastModified = Clock.System.now(),
            patientUnreadMessageCount = 0,
            providersInConversation = emptyList(),
            pictureUrl = null,
            isLocked = false,
        )
    }

    sealed interface CreationState {
        object ToBeCreated : CreationState
        object Creating : CreationState
        data class ErrorCreating(val cause: Throwable) : CreationState
        data class Created(val remoteId: ConversationId.Remote) : CreationState

        fun requireCreated(message: String): Created = this as? Created ?: throwNablaInternalException(message)
    }
}
