package com.nabla.sdk.messaging.core.data.stubs

import com.auth0.android.jwt.JWT
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.BaseFileUpload
import com.nabla.sdk.core.domain.entity.EphemeralUrl
import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.messaging.core.domain.entity.BaseMessage
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivity
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivityContent
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItems
import com.nabla.sdk.messaging.core.domain.entity.FileLocal
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.domain.entity.toConversationActivityId
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
    mediaSource: FileSource<FileLocal.Image, FileUpload.Image> = FileSource.Uploaded.fakeImage(),
) = Message.Media.Image(
    baseMessage = BaseMessage(
        id = id,
        createdAt = sentAt,
        sender = sender,
        sendStatus = status,
        conversationId = conversationId,
    ),
    mediaSource = mediaSource,
)

internal fun FileSource.Local.Companion.fakeImage(
    fileLocal: FileLocal.Image = FileLocal.Image.fake()
) = FileSource.Local<FileLocal.Image, FileUpload.Image>(
    fileLocal = fileLocal,
)

internal fun FileSource.Uploaded.Companion.fakeImage(
    fileLocal: FileLocal.Image? = FileLocal.Image.fake(),
    fileUpload: FileUpload.Image = FileUpload.Image.fake()
) = FileSource.Uploaded<FileLocal.Image, FileUpload.Image>(
    fileLocal = fileLocal,
    fileUpload = fileUpload,
)

internal fun FileLocal.Image.Companion.fake(
    uri: Uri = Uri("contentprovider:image.png")
) = FileLocal.Image(
    uri = uri,
    imageName = "image.png",
    mimeType = MimeType.Image.JPEG
)

internal fun FileUpload.Image.Companion.fake(
    ephemeralUrl: EphemeralUrl = EphemeralUrl.fake(url = Uri("https://i.pravatar.cc/900")),
    mimeType: MimeType = MimeType.Image.JPEG,
    fileName: String = "filename.jpg",
) = FileUpload.Image(
    size = null,
    fileUpload = BaseFileUpload(
        id = uuid4(),
        url = ephemeralUrl,
        mimeType = mimeType,
        fileName = fileName,
    )
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
    typingAt: Instant? = null,
    seenUntil: Instant = Clock.System.now(),
) = ProviderInConversation(
    provider = provider,
    typingAt = typingAt,
    seenUntil = seenUntil,
)

fun Conversation.Companion.fake(
    id: Uuid = uuid4(),
    title: String = randomText(maxWords = 10),
    inboxPreviewTitle: String = randomText(maxWords = 10),
    description: String = listOf("", "You: oh great!", "You: image", "Doctor is typing...").random(),
    lastModified: Instant = Clock.System.now().minus(2.minutes),
    lastMessagePreview: String = randomText(maxWords = 10),
    patientUnreadMessageCount: Int = 0,
    providersInConversation: List<ProviderInConversation> = listOf(ProviderInConversation.fake()),
) = Conversation(
    id = id.toConversationId(),
    lastModified = lastModified,
    title = title,
    inboxPreviewTitle = inboxPreviewTitle,
    description = description,
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

internal fun ConversationActivity.Companion.fakeProviderJoined() = ConversationActivity(
    id = uuid4().toConversationActivityId(),
    conversationId = uuid4().toConversationId(),
    createdAt = Clock.System.now(),
    activityTime = Clock.System.now(),
    content = ConversationActivityContent.ProviderJoinedConversation(
        maybeProvider = User.Provider.fake()
    )
)

internal fun AuthTokens.Companion.fake() = AuthTokens(
    refreshToken = Jwt.expiredIn2050,
    accessToken = Jwt.expiredIn2050_2,
)

internal object Jwt {
    // Use https://www.javainuse.com/jwtgenerator to easily generate mocked tokens
    const val expiredIn2050 = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiUm9sZSIsIklzc3VlciI6Iklzc3VlciIsIlVzZXJuYW1lIjoiVXNlcm5hbWUiLCJleHAiOjI1MjE4NDMyMDAsImlhdCI6OTQzOTIwMDAwfQ.a9B-ZzVUPI04w6AjKZ9ODvU7P8s4G6SqpQnfaei5EaE"
    const val expiredIn2050_2 = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiUm9sZSIsIklzc3VlciI6Iklzc3VlciIsIlVzZXJuYW1lIjoiVXNlcm5hbWUiLCJleHAiOjI1MjE4NDMyMDAsImlhdCI6OTQzOTIwMDAwfQ.nqK7fOSd0WcVk3HYlbQuK8jindWlao4QTp8E2CWhIdg"
    const val expiredIn2050_3 = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiUm9sZSIsIklzc3VlciI6Iklzc3VlciIsIlVzZXJuYW1lIjoiVXNlcm5hbWUiLCJleHAiOjI1MjE4NDMyMDAsImlhdCI6OTQzOTIwMDAwfQ.SBykkJNK3avicHjw16uHCxFUYmbp_YpLc34YsC31eu0"
    const val expiredIn2020 = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiUm9sZSIsIklzc3VlciI6Iklzc3VlciIsIlVzZXJuYW1lIjoiVXNlcm5hbWUiLCJleHAiOjE1NzUwNzIwMDAsImlhdCI6OTQzOTIwMDAwfQ.tITlAVJAI8LX1Fi0FStSJWf5z45Vs8mXoXlfpaTnR9c"
}

internal fun String.toJwt() = JWT(this)

private fun nowMinus(duration: Duration): Instant = Clock.System.now().minus(duration)

private fun randomText(maxWords: Int? = null) =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
        .split(" ")
        .shuffled()
        .let { it.take(Random.nextInt().absoluteValue % it.size.coerceAtMost(maxWords ?: Int.MAX_VALUE) + 1) }
        .joinToString(separator = " ")
