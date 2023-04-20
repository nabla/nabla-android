package com.nabla.sdk.messaging

import com.nabla.sdk.core.data.stubs.fake
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.SystemUser
import com.nabla.sdk.messaging.core.data.stubs.fake
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation.Companion.TYPING_TIME_WINDOW
import com.nabla.sdk.messaging.ui.scene.messages.MessageAction
import com.nabla.sdk.messaging.ui.scene.messages.PlaybackProgress
import com.nabla.sdk.messaging.ui.scene.messages.TimelineBuilder
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import kotlinx.datetime.Clock
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimelineBuilderTest {

    @Test
    fun `When message is selected then its status is shown`() {
        val message1 = Message.Text.fake()
        val message2 = Message.Text.fake()
        val messages = listOf(message1, message2)

        val outputMessages = TimelineBuilder().buildTimeline(
            items = messages,
            selectedMessageId = message1.id,
            hasMore = false,
            providersInConversation = emptyList(),
        ).filterIsInstance<TimelineItem.Message>()

        assertTrue(outputMessages.first { it.id == message1.id }.showStatus, "expected selected message to show status")
        assertTrue(outputMessages.first().showStatus, "expected latest patient message to show status")
        assertFalse(outputMessages.last().showStatus, "expected first message not to show status")
    }

    @Test
    fun `When two consecutive messages are from same provider then second message doesn't show avatar`() {
        val message1 = Message.Text.fake(author = MessageAuthor.Provider(Provider.fake()))
        val message2 = Message.Text.fake(author = message1.author, sentAt = message1.sentAt.plus(1.seconds))
        val messages = listOf(message2, message1)

        val outputMessages = TimelineBuilder().buildTimeline(
            items = messages,
            hasMore = false,
            providersInConversation = emptyList(),
        ).filterIsInstance<TimelineItem.Message>()

        assertTrue(outputMessages.last().showAuthorAvatar, "expected oldest message to show avatar")
        assertFalse(outputMessages.first().showAuthorAvatar, "expected newest message not to show avatar")
    }

    @Test
    fun `When typingDoctor is not empty then typing indicator is shown`() {
        val message1 = Message.Text.fake()
        val message2 = Message.Text.fake(sentAt = message1.sentAt.plus(1.seconds))
        val messages = listOf(message2, message1)
        val provider = Provider.fake()
        val providerInConversation = ProviderInConversation.fake(provider = provider, typingAt = Clock.System.now())

        val outputMessages = TimelineBuilder().buildTimeline(
            items = messages,
            hasMore = false,
            providersInConversation = listOf(providerInConversation),
        )
        val outputFirstMessage = outputMessages.first()

        assertTrue(outputFirstMessage is TimelineItem.ProviderTypingIndicator, "expected typing indicator")
        assertTrue(outputFirstMessage.showProviderName, "expected typing indicator name to be shown since previous message is not from same doctor")
    }

    @Test
    fun `When typingDoctor is outdated then no typing indicator is shown`() {
        val message1 = Message.Text.fake(sentAt = Clock.System.now().minus(30.minutes))
        val message2 = Message.Text.fake(sentAt = message1.sentAt.plus(100.milliseconds))
        val messages = listOf(message2, message1)
        val provider = Provider.fake()
        val providerInConversation = ProviderInConversation.fake(
            provider = provider,
            typingAt = Clock.System.now().minus(TYPING_TIME_WINDOW.plus(1.seconds)),
        )

        val outputMessages = TimelineBuilder().buildTimeline(
            items = messages,
            hasMore = false,
            providersInConversation = listOf(providerInConversation),
        )
        val outputFirstMessage = outputMessages.first()

        assertTrue(
            outputFirstMessage !is TimelineItem.ProviderTypingIndicator,
            "expected no typing indicator",
        )
    }

    @Test
    fun `When not all items are loaded then a loader is shown`() {
        val messages = listOf(Message.Text.fake())

        val outputMessages = TimelineBuilder().buildTimeline(
            items = messages,
            hasMore = true,
            providersInConversation = emptyList(),
        )
        val outputLastMessage = outputMessages.last()

        assertTrue(outputLastMessage is TimelineItem.LoadingMore, "expected loading more at end of list")
    }

    @Test
    fun `Audio message has its progress from the progress map`() {
        val audioMessage = Message.Media.Audio.fake()
        val progress = PlaybackProgress(currentPositionMillis = 5_123, totalDurationMillis = 25_789)

        val outputMessages = TimelineBuilder().buildTimeline(
            items = listOf(audioMessage),
            hasMore = false,
            providersInConversation = emptyList(),
            audioPlaybackProgressMap = mapOf(audioMessage.stableUri to progress),
        )
        val outputContent = outputMessages.filterIsInstance<TimelineItem.Message>().first().content as TimelineItem.Message.Audio

        assertEquals(
            progress,
            outputContent.progress,
            "expected audio message item to have progress from the map",
        )
    }

    @Test
    fun `Audio message has its progress from estimated duration if map is empty`() {
        val audioMessage = Message.Media.Audio.fake()

        val outputMessages = TimelineBuilder().buildTimeline(
            items = listOf(audioMessage),
            hasMore = false,
            providersInConversation = emptyList(),
            audioPlaybackProgressMap = emptyMap(),
        )
        val outputContent = outputMessages.filterIsInstance<TimelineItem.Message>().first().content as TimelineItem.Message.Audio

        assertEquals(
            PlaybackProgress(currentPositionMillis = 0, audioMessage.durationMs),
            outputContent.progress,
            "expected audio message progress to reflect estimation duration",
        )
    }

    @Test
    fun `Now playing audio is set to playing`() {
        val audioMessage1 = Message.Media.Audio.fake()
        val audioMessage2 = Message.Media.Audio.fake()

        val outputMessages = TimelineBuilder().buildTimeline(
            items = listOf(audioMessage1, audioMessage2),
            nowPlayingAudioUri = audioMessage1.stableUri,
            hasMore = false,
            providersInConversation = emptyList(),
        )
        val (outputContent1, outputContent2) = outputMessages
            .filterIsInstance<TimelineItem.Message>()
            .let {
                it.first { it.id == audioMessage1.id }.content as TimelineItem.Message.Audio to
                    it.first { it.id == audioMessage2.id }.content as TimelineItem.Message.Audio
            }

        assertTrue(outputContent1.isPlaying, "expected audio message to be marked playing")
        assertFalse(outputContent2.isPlaying, "expected audio message to not be marked playing")
    }

    @Test
    fun `Patient message has Delete, Copy and Reply actions`() {
        val outputMessage = TimelineBuilder().buildTimeline(
            items = listOf(Message.Text.fake(author = MessageAuthor.Patient.Current)),
            hasMore = false,
            providersInConversation = emptyList(),
        ).filterIsInstance<TimelineItem.Message>().first()

        assertEquals(
            setOf(MessageAction.Copy, MessageAction.Delete, MessageAction.Reply),
            outputMessage.actions,
            "wrong actions on message",
        )
    }

    @Test
    fun `Provider message has Copy and Reply actions`() {
        val outputMessage = TimelineBuilder().buildTimeline(
            items = listOf(
                Message.Text.fake(author = MessageAuthor.Provider(Provider.fake())),
            ),
            hasMore = false,
            providersInConversation = emptyList(),
        ).filterIsInstance<TimelineItem.Message>().first()

        assertEquals(setOf(MessageAction.Copy, MessageAction.Reply), outputMessage.actions, "wrong actions on message")
    }

    @Test
    fun `System message has Copy and Reply actions`() {
        val outputMessage = TimelineBuilder().buildTimeline(
            items = listOf(
                Message.Text.fake(author = MessageAuthor.System(SystemUser.fake())),
            ),
            hasMore = false,
            providersInConversation = emptyList(),
        ).filterIsInstance<TimelineItem.Message>().first()

        assertEquals(setOf(MessageAction.Copy, MessageAction.Reply), outputMessage.actions, "wrong actions on message")
    }
}
