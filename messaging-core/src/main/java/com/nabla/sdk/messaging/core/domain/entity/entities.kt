package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.AttachmentId
import com.nabla.sdk.core.domain.entity.PatientId
import com.nabla.sdk.core.domain.entity.ProviderId
import com.nabla.sdk.core.domain.entity.UserId
import kotlinx.datetime.Instant

typealias ConversationId = String
data class Conversation(
    val id: ConversationId,
    val title: String,
    val patientId: PatientId,
    val providerIds: List<ProviderId>,
)

typealias MessageId = String
data class Message(
    val id: MessageId,
    val userId: UserId,
    val text: String,
    val attachment: AttachmentId?,
    val createdAt: Instant,
)
