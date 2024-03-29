package com.nabla.sdk.messaging

import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.data.stubs.StringFaker.randomText
import com.nabla.sdk.core.data.stubs.UriFaker
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.ui.scene.messages.PlaybackProgress
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal fun TimelineItem.Message.Companion.fake(
    id: MessageId = MessageId.Remote(uuid4(), uuid4()),
    sendStatus: SendStatus = SendStatus.Sent,
    showStatus: Boolean = false,
    time: Instant = Clock.System.now(),
    author: TimelineItem.Message.Author = TimelineItem.Message.Author.CurrentPatient,
    showAuthorAvatar: Boolean = false,
    showAuthorName: Boolean = false,
    content: TimelineItem.Message.Content,
) = TimelineItem.Message(
    id = id,
    author = author,
    showAuthorAvatar = showAuthorAvatar,
    showAuthorName = showAuthorName,
    status = sendStatus,
    showStatus = showStatus,
    time = time,
    actions = emptySet(),
    content = content,
)

internal fun TimelineItem.Message.Audio.Companion.fake() = TimelineItem.Message.Audio(
    uri = UriFaker.remote(),
    progress = PlaybackProgress.UNKNOWN,
    isPlaying = false,
)

internal fun TimelineItem.Message.Text.Companion.fake() = TimelineItem.Message.Text(
    text = randomText(),
    repliedMessage = null,
)

internal fun TimelineItem.Message.Image.Companion.fake() = TimelineItem.Message.Image(
    uri = UriFaker.remote(),
)

internal fun TimelineItem.Message.Video.Companion.fake() = TimelineItem.Message.Video(
    uri = UriFaker.remote(),
)
