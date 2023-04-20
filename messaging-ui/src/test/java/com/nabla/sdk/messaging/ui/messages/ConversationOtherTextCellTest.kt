package com.nabla.sdk.messaging.ui.messages

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.android.ide.common.rendering.api.SessionParams
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.fake
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays
import com.nabla.sdk.messaging.ui.scene.messages.RepliedMessage
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherTextMessageViewHolder
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

internal class ConversationOtherTextCellTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule(
        renderingMode = SessionParams.RenderingMode.V_SCROLL,
    )

    @Test
    fun `test other short text`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "This is a short text",
            repliedMessage = null,
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test other long text`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = null,
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test other short text responding to a short patient text`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedPatientTextMessage("Short text first message"),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test other long text responding to a short patient text`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedPatientTextMessage("Short text first message"),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test other short text responding to a long patient text`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedPatientTextMessage(longText),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test other long text responding to a long patient text`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedPatientTextMessage(longText),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test other short text responding to an image patient sent`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedPatientImageMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test other long text responding to an image patient sent`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedPatientImageMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test other short text responding to a video patient sent`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedPatientVideoMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test other long text responding to a video patient sent`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedPatientVideoMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test other short text responding to a document patient sent`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedPatientDocumentMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test other long text responding to a document patient sent`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedPatientDocumentMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test other short text responding to an audio patient sent`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedPatientAudioMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test other long text responding to an audio patient sent`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createOtherTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedPatientAudioMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    private fun makeRepliedPatientTextMessage(text: String) = makeRepliedPatientMessage(RepliedMessage.Content.Text(text))

    private fun makeRepliedPatientImageMessage() = makeRepliedPatientMessage(RepliedMessage.Content.Image(Uri("https://google.com")))

    private fun makeRepliedPatientVideoMessage() = makeRepliedPatientMessage(RepliedMessage.Content.Video(Uri("https://google.com")))

    private fun makeRepliedPatientDocumentMessage() = makeRepliedPatientMessage(
        RepliedMessage.Content.Document(
            uri = Uri("https://google.com"),
            thumbnailUri = Uri("https://google.com"),
        ),
    )

    private fun makeRepliedPatientAudioMessage() = makeRepliedPatientMessage(RepliedMessage.Content.Audio(Uri("https://google.com")))

    private fun makeRepliedPatientMessage(content: RepliedMessage.Content) = RepliedMessage(
        id = MessageId.Remote(Uuid.randomUUID(), Uuid.randomUUID()),
        content = content,
        author = TimelineItem.Message.Author.CurrentPatient,
    )

    private fun OtherTextMessageViewHolder.bindFakeTextContent(content: TimelineItem.Message.Text) {
        val author = TimelineItem.Message.Author.Other(
            uuid = Uuid.randomUUID(),
            displayName = "Luigi",
            avatar = null,
        )

        bind(
            TimelineItem.Message.fake(
                sendStatus = SendStatus.Sent,
                showStatus = true,
                showAuthorAvatar = true,
                showAuthorName = true,
                time = Instant.DISTANT_PAST,
                content = content,
                author = author,
            ),
            author,
            content,
        )
    }

    private fun createOtherTextViewHolder(context: Context, layoutInflater: LayoutInflater): Pair<View, OtherTextMessageViewHolder> {
        val parent = FrameLayout(context.withNablaMessagingThemeOverlays())
        parent.setPadding(0, 50, 0, 50)
        val viewHolder = OtherTextMessageViewHolder.create(
            inflater = layoutInflater.cloneInContext(context.withNablaMessagingThemeOverlays()),
            parent = parent,
            onUrlClicked = {},
            onRepliedMessageClicked = {},
        )

        parent.addView(viewHolder.itemView)

        return Pair(parent, viewHolder)
    }

    private companion object {
        const val longText = "This is a super long text with multiple sentences, that are long, and a smiley: \uD83D\uDE00 and an url: https://www.google/com and a phone number +44663929491 . Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."
    }
}
