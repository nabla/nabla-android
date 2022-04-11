package com.nabla.sdk.messaging.core.domain.entity

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.domain.entity.Attachment
import com.nabla.sdk.core.domain.entity.BaseFileUpload
import com.nabla.sdk.core.domain.entity.EphemeralUrl
import com.nabla.sdk.core.domain.entity.FileUpload
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
    id: MessageId = MessageId.Remote(uuid4(), uuid4()),
    sentAt: Instant = Clock.System.now().minus(20.minutes),
    sender: MessageSender = MessageSender.Patient,
    status: SendStatus = SendStatus.Sent,
    text: String = "message content",
) = Message.Text(
    BaseMessage(
        id = id,
        sentAt = sentAt,
        sender = sender,
        sendStatus = status,
        conversationId = uuid4().toConversationId(),
    ),
    text = text,
)

internal fun Message.Media.Image.Companion.fake(
    id: MessageId = MessageId.Remote(uuid4(), uuid4()),
    sentAt: Instant = Clock.System.now().minus(20.minutes),
    sender: MessageSender = MessageSender.Patient,
    status: SendStatus = SendStatus.Sent,
    conversationId: ConversationId = ConversationId(uuid4()),
    ephemeralUrl: EphemeralUrl = EphemeralUrl.fake(url = Uri("https://i.pravatar.cc/900")),
    mimeType: MimeType = MimeType.Image.JPEG,
    fileName: String = "filename.jpg",
) = Message.Media.Image(
    message = BaseMessage(
        id = id,
        sentAt = sentAt,
        sender = sender,
        sendStatus = status,
        conversationId = conversationId,
    ),
    mediaSource = FileSource.Uploaded(
        fileLocal = null,
        fileUpload = FileUpload.Image(
            width = 300,
            height = 300,
            fileUpload = BaseFileUpload(
                id = uuid4(),
                url = ephemeralUrl,
                mimeType = mimeType,
                fileName = fileName,
            )
        ),
    ),
)

internal fun Message.Media.Document.Companion.fake(
    id: MessageId = MessageId.Remote(uuid4(), uuid4()),
    sentAt: Instant = Clock.System.now().minus(20.minutes),
    sender: MessageSender = MessageSender.Patient,
    status: SendStatus = SendStatus.Sent,
    conversationId: ConversationId = ConversationId(uuid4()),
    ephemeralUrl: EphemeralUrl = EphemeralUrl.fake(url = Uri("https://www.orimi.com/pdf-test.pdf")),
    mimeType: MimeType = MimeType.Application.PDF,
    fileName: String = "filename.jpg",
    thumbnail: FileUpload.Image? = null,
) = Message.Media.Document(
    message = BaseMessage(
        id = id,
        sentAt = sentAt,
        sender = sender,
        sendStatus = status,
        conversationId = conversationId,
    ),
    mediaSource = FileSource.Uploaded(
        fileLocal = null,
        fileUpload = FileUpload.Document(
            fileUpload = BaseFileUpload(
                id = uuid4(),
                url = ephemeralUrl,
                mimeType = mimeType,
                fileName = fileName,
            ),
            thumbnail = thumbnail
        ),
    ),
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
        mimeType = MimeType.Image.JPEG,
        thumbnailUrl = Uri("https://i.pravatar.cc/300"),
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

internal fun EphemeralUrl.Companion.fake(
    expiresAt: Instant = Instant.DISTANT_FUTURE,
    url: Uri,
) = EphemeralUrl(
    expiresAt = expiresAt,
    url = url,
)

private fun nowMinus(duration: Duration): Instant = Clock.System.now().minus(duration)
