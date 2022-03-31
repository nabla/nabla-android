package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.Id
import kotlinx.datetime.Instant

data class Conversation(
    val id: Id,
    val inboxPreviewTitle: String,
    val inboxPreviewSubtitle: String,
    val lastModified: Instant,
    val patientUnreadMessageCount: Int,
    val providersInConversation: List<ProviderInConversation>,
) {
    companion object
}
