package com.nabla.sdk.messaging.core.data.stubs

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.domain.entity.BaseFileUpload
import com.nabla.sdk.core.domain.entity.EphemeralUrl
import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Size
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.messaging.core.domain.entity.BaseMessage
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItems
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun ConversationItems.Companion.fake(
    conversation: Conversation = Conversation.fake(),
    messages: List<Message> = ((5 downTo 1).map { Message.Text.fake(sentAt = nowMinus(it.minutes)) }),
) = ConversationItems(
    conversationId = conversation.id,
    items = messages,
)

internal fun Message.Text.Companion.fake(
    id: MessageId = MessageId.Remote(uuid4(), uuid4()),
    sentAt: Instant = Clock.System.now().minus(20.minutes),
    sender: MessageSender = MessageSender.Patient,
    status: SendStatus = SendStatus.Sent,
    text: String = randomText(),
) = Message.Text(
    BaseMessage(
        id = id,
        createdAt = sentAt,
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
    baseMessage = BaseMessage(
        id = id,
        createdAt = sentAt,
        sender = sender,
        sendStatus = status,
        conversationId = conversationId,
    ),
    mediaSource = FileSource.Uploaded(
        fileLocal = null,
        fileUpload = FileUpload.Image(
            size = Size(300, 300),
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
    fileName: String = "filename.pdf",
    thumbnail: FileUpload.Image? = null,
) = Message.Media.Document(
    baseMessage = BaseMessage(
        id = id,
        createdAt = sentAt,
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

fun User.Provider.Companion.fake(
    id: Uuid = uuid4(),
    firstName: String = "Véronique",
    lastName: String = "Cayol",
    prefix: String? = "Dr",
    avatar: EphemeralUrl? = EphemeralUrl(
        expiresAt = Instant.DISTANT_FUTURE,
        url = Uri("https://i.pravatar.cc/300"),
    ),
) = User.Provider(
    id = id,
    avatar = avatar,
    firstName = firstName,
    lastName = lastName,
    prefix = prefix,
)

fun ProviderInConversation.Companion.fake(
    provider: User.Provider = User.Provider.fake(),
    typingAt: Instant? = Random.nextBoolean().let { if (it) Clock.System.now() else null },
    seenUntil: Instant = Clock.System.now(),
) = ProviderInConversation(
    provider = provider,
    typingAt = typingAt,
    seenUntil = seenUntil,
)

fun Conversation.Companion.fake(
    id: Uuid = uuid4(),
    inboxPreviewTitle: String = randomText(maxWords = 10),
    inboxPreviewSubtitle: String = listOf("", "You: oh great!", "You: image", "Doctor is typing...").random(),
    lastModified: Instant = Clock.System.now().minus(2.minutes),
    lastMessagePreview: String = "Bonjour:",
    patientUnreadMessageCount: Int = 0,
    providersInConversation: List<ProviderInConversation> = listOf(ProviderInConversation.fake()),
) = Conversation(
    id = id.toConversationId(),
    lastModified = lastModified,
    title = inboxPreviewTitle,
    inboxPreviewTitle = inboxPreviewTitle,
    description = inboxPreviewSubtitle,
    lastMessagePreview = lastMessagePreview,
    patientUnreadMessageCount = patientUnreadMessageCount,
    providersInConversation = providersInConversation,
)

fun EphemeralUrl.Companion.fake(
    expiresAt: Instant = Instant.DISTANT_FUTURE,
    url: Uri,
) = EphemeralUrl(
    expiresAt = expiresAt,
    url = url,
)

private fun nowMinus(duration: Duration): Instant = Clock.System.now().minus(duration)

private fun randomText(maxWords: Int? = null) =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
        .split(" ")
        .shuffled()
        .let { it.take(Random.nextInt().absoluteValue % it.size.coerceAtMost(maxWords ?: Int.MAX_VALUE) + 1) }
        .joinToString(separator = " ")
