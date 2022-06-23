package com.nabla.sdk.messaging.core.domain.entity

import androidx.annotation.VisibleForTesting
import com.benasher44.uuid.Uuid
import kotlinx.datetime.Instant

@JvmInline
public value class ConversationId @VisibleForTesting public constructor(public val value: Uuid)

public fun Uuid.toConversationId(): ConversationId = ConversationId(this)

public data class Conversation(
    val id: ConversationId,
    val title: String?,
    val subtitle: String?,
    val inboxPreviewTitle: String,
    val lastMessagePreview: String?,
    val lastModified: Instant,
    val patientUnreadMessageCount: Int,
    val providersInConversation: List<ProviderInConversation>,
) {
    public companion object
}
