package com.nabla.sdk.messaging.core.domain.entity

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.domain.entity.Attachment
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.domain.entity.User
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Ultimately we'll move these mockers to test srcSet to help with UTs.
 */

internal fun ConversationWithMessages.Companion.fake(
    conversation: Conversation = Conversation.fake(),
    messages: PaginatedList<Message> = PaginatedList((5 downTo 1).map { Message.Text.fake(sentAt = nowMinus(it.minutes)) }, hasMore = true),
) = ConversationWithMessages(
    conversation = conversation,
    messages = messages,
)

internal fun Message.Text.Companion.fake(
    localId: Uuid = uuid4(),
    remoteId: Uuid = uuid4(),
    sentAt: Instant = Clock.System.now().minus(20.minutes),
    sender: MessageSender = MessageSender.Patient,
    status: MessageStatus = MessageStatus.Sent,
    text: String = "message content",
) = Message.Text(
    BaseMessage(
        id = MessageId.Remote(localId, remoteId),
        sentAt = sentAt,
        sender = sender,
        status = status,
        conversationId = uuid4().toConversationId(),
    ),
    text = text,
)

internal fun User.Provider.Companion.fake(
    id: Uuid = uuid4(),
    firstName: String = "Véronique",
    lastName: String = "Cayol",
    title: String? = "Gynécologue",
    prefix: String? = "Dr",
    avatar: Attachment? = Attachment(
        uuid4(),
        url = Uri("https://i.pravatar.cc/300"),
        mimeType = MimeType.Generic("image/png"),
        thumbnailUrl = Uri(""),
    ),
) = User.Provider(
    id = id,
    avatar = avatar,
    firstName = firstName,
    lastName = lastName,
    title = title,
    prefix = prefix,
)

internal fun ProviderInConversation.Companion.fake(
    provider: User.Provider = User.Provider.fake(),
    isTyping: Boolean = true,
    seenUntil: Instant = Clock.System.now(),
) = ProviderInConversation(
    provider = provider,
    isTyping = isTyping,
    seenUntil = seenUntil,
)

internal fun Conversation.Companion.fake(
    id: Uuid = uuid4(),
    inboxPreviewTitle: String = "title ${Random.nextInt()}",
    inboxPreviewSubtitle: String = "subtitle",
    lastModified: Instant = Clock.System.now().minus(2.minutes),
    patientUnreadMessageCount: Int = 0,
    providersInConversation: List<ProviderInConversation> = listOf(ProviderInConversation.fake()),
) = Conversation(
    id = id.toConversationId(),
    inboxPreviewTitle = inboxPreviewTitle,
    inboxPreviewSubtitle = inboxPreviewSubtitle,
    lastModified = lastModified,
    patientUnreadMessageCount = patientUnreadMessageCount,
    providersInConversation = providersInConversation,
)

private fun nowMinus(duration: Duration): Instant = Clock.System.now().minus(duration)
