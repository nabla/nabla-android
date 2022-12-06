package com.nabla.sdk.messaging.ui.messages

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.stubs.fake
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.fake
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays
import com.nabla.sdk.messaging.ui.scene.messages.RepliedMessage
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientTextMessageViewHolder
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

internal class ConversationPatientTextCellTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule()

    @Test
    fun `test patient short text`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "This is a short text",
            repliedMessage = null,
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test patient long text`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = null,
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test patient short text responding to a short provider text`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedProviderTextMessage("Short text first message"),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test patient long text responding to a short provider text`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedProviderTextMessage("Short text first message"),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test patient short text responding to a long provider text`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedProviderTextMessage(longText),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test patient long text responding to a long provider text`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedProviderTextMessage(longText),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test patient short text responding to an image provider sent`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedProviderImageMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test patient long text responding to an image provider sent`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedProviderImageMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test patient short text responding to a video provider sent`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedProviderVideoMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test patient long text responding to a video provider sent`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedProviderVideoMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test patient short text responding to a document provider sent`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedProviderDocumentMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test patient long text responding to a document provider sent`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedProviderDocumentMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    @Test
    fun `test patient short text responding to an audio provider sent`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = "Short text response",
            repliedMessage = makeRepliedProviderAudioMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test patient long text responding to an audio provider sent`() = paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
        val (parent, viewHolder) = createPatientTextViewHolder(context, layoutInflater)

        val content = TimelineItem.Message.Text(
            text = longText,
            repliedMessage = makeRepliedProviderAudioMessage(),
        )

        viewHolder.bindFakeTextContent(content)

        return@snapshotDayNightMultiDevices parent
    }

    private fun makeRepliedProviderTextMessage(text: String) = makeRepliedProviderMessage(RepliedMessage.Content.Text(text))

    private fun makeRepliedProviderImageMessage() = makeRepliedProviderMessage(RepliedMessage.Content.Image(Uri("https://google.com")))

    private fun makeRepliedProviderVideoMessage() = makeRepliedProviderMessage(RepliedMessage.Content.Video(Uri("https://google.com")))

    private fun makeRepliedProviderDocumentMessage() = makeRepliedProviderMessage(
        RepliedMessage.Content.Document(
            uri = Uri("https://google.com"),
            thumbnailUri = Uri("https://google.com")
        ),
    )

    private fun makeRepliedProviderAudioMessage() = makeRepliedProviderMessage(RepliedMessage.Content.Audio(Uri("https://google.com")))

    private fun makeRepliedProviderMessage(content: RepliedMessage.Content) = RepliedMessage(
        id = MessageId.Remote(Uuid.randomUUID(), Uuid.randomUUID()),
        content = content,
        author = providerAuthor,
    )

    private fun PatientTextMessageViewHolder.bindFakeTextContent(content: TimelineItem.Message.Text) {
        bind(
            TimelineItem.Message.fake(
                sendStatus = SendStatus.Sent,
                showStatus = true,
                time = Instant.DISTANT_PAST,
                content = content,
            ),
            TimelineItem.Message.Author.CurrentPatient,
            content,
        )
    }

    private fun createPatientTextViewHolder(context: Context, layoutInflater: LayoutInflater): Pair<View, PatientTextMessageViewHolder> {
        val parent = FrameLayout(context.withNablaMessagingThemeOverlays())
        parent.setPadding(0, 50, 0, 50)
        val viewHolder = PatientTextMessageViewHolder.create(
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
        val providerAuthor = TimelineItem.Message.Author.Provider(
            provider = Provider.fake(
                firstName = "Mario",
                lastName = "Bros"
            ),
        )
    }
}
