package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.Attachment
import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.core.domain.entity.User
import kotlinx.datetime.Instant

data class Conversation(
    val id: Id,
    val inboxPreviewTitle: String,
    val inboxPreviewSubtitle: String,
    val lastModified: Instant,
    val patientUnreadMessageCount: Int,
    val providers: List<User.Provider>,
)

data class Message(
    val id: Id,
    val author: User,
    val text: String,
    val attachment: Attachment?,
    val createdAt: Instant,
)
