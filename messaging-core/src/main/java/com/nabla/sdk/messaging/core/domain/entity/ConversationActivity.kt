package com.nabla.sdk.messaging.core.domain.entity

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.MaybeProvider
import kotlinx.datetime.Instant

@JvmInline
public value class ConversationActivityId internal constructor(public val value: Uuid)

public fun Uuid.toConversationActivityId(): ConversationActivityId = ConversationActivityId(this)

public data class ConversationActivity(
    val id: ConversationActivityId,
    val conversationId: ConversationId,
    override val createdAt: Instant,
    val activityTime: Instant,
    val content: ConversationActivityContent
) : ConversationItem {
    public companion object
}

public sealed class ConversationActivityContent {
    public data class ProviderJoinedConversation(
        val maybeProvider: MaybeProvider
    ) : ConversationActivityContent() {
        public companion object
    }
}
