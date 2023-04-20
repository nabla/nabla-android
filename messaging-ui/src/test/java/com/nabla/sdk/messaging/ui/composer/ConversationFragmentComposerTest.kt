package com.nabla.sdk.messaging.ui.composer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.stubs.fake
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.core.ui.helpers.mediapicker.LocalMedia
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaFragmentConversationBinding
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays
import com.nabla.sdk.messaging.ui.scene.messages.RepliedMessage
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.loadReplyContentThumbnailOrHide
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.repliedToAuthorName
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.repliedToContent
import com.nabla.sdk.messaging.ui.scene.messages.editor.MediaToSendAdapter
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import org.junit.Rule
import org.junit.Test
import java.net.URI

class ConversationFragmentComposerTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule()

    @Test
    fun `Test empty default composer`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent) = createBinding(context, layoutInflater)

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test empty and medias composer`() {
        paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.setMediaToSend(mediasToSend)

            return@snapshotDayNightMultiDevices parent
        }
    }

    @Test
    fun `Test filled with short text default composer`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.conversationEditText.setText("Small text")

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test filled with long text default composer`() {
        paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.conversationEditText.setText(longText)

            return@snapshotDayNightMultiDevices parent
        }
    }

    @Test
    fun `Test filled with short text and medias composer`() {
        paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.conversationEditText.setText("Small text")
            binding.setMediaToSend(mediasToSend)

            return@snapshotDayNightMultiDevices parent
        }
    }

    @Test
    fun `Test filled with long text and medias composer`() {
        paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.conversationEditText.setText(longText)
            binding.setMediaToSend(mediasToSend)

            return@snapshotDayNightMultiDevices parent
        }
    }

    @Test
    fun `Test replying to text message with long text default composer`() {
        paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.conversationEditText.setText(longText)

            val repliedMessage = makeRepliedProviderTextMessage("Hey this is a text to reply to from a Provider that is a bit long but ok")
            binding.setRepliedMessage(repliedMessage)

            return@snapshotDayNightMultiDevices parent
        }
    }

    @Test
    fun `Test replying to image message with long text default composer`() {
        paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.conversationEditText.setText(longText)

            val repliedMessage = makeRepliedProviderImageMessage()
            binding.setRepliedMessage(repliedMessage)

            return@snapshotDayNightMultiDevices parent
        }
    }

    @Test
    fun `Test replying to video message with long text default composer`() {
        paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.conversationEditText.setText(longText)

            val repliedMessage = makeRepliedProviderVideoMessage()
            binding.setRepliedMessage(repliedMessage)

            return@snapshotDayNightMultiDevices parent
        }
    }

    @Test
    fun `Test replying to document message with long text default composer`() {
        paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.conversationEditText.setText(longText)

            val repliedMessage = makeRepliedProviderDocumentMessage()
            binding.setRepliedMessage(repliedMessage)

            return@snapshotDayNightMultiDevices parent
        }
    }

    @Test
    fun `Test replying to audio message with long text default composer`() {
        paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.conversationEditText.setText(longText)

            val repliedMessage = makeRepliedProviderAudioMessage()
            binding.setRepliedMessage(repliedMessage)

            return@snapshotDayNightMultiDevices parent
        }
    }

    @Test
    fun `Test replying to text message with long text and medias composer`() {
        paparazzi.snapshotDayNightMultiDevices { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.conversationEditText.setText(longText)
            binding.setMediaToSend(mediasToSend)

            val repliedMessage = makeRepliedProviderTextMessage("Hey this is a text to reply to from a Provider that is a bit long but ok")
            binding.setRepliedMessage(repliedMessage)

            return@snapshotDayNightMultiDevices parent
        }
    }

    @Test
    fun `Test audio recording composer`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.setRecordingVoice(minutes = 3, seconds = 16)

            return@snapshotDayNightDefaultDevice parent
        }
    }

    private fun NablaFragmentConversationBinding.setRecordingVoice(minutes: Int, seconds: Int) {
        conversationCancelRecordingButton.isVisible = true
        conversationRecordingVoiceProgress.isVisible = true
        conversationRecordVoiceButton.isVisible = false

        conversationRecordingVoiceProgressText.text = context.getString(R.string.nabla_conversation_audio_message_seconds_format, minutes, seconds)
    }

    private fun NablaFragmentConversationBinding.setRepliedMessage(repliedMessage: RepliedMessage) {
        currentlyReplyingToLayout.isVisible = true
        currentlyReplyingToBody.text = repliedMessage.repliedToContent(context)
        currentlyReplyingToTitle.text = context.getString(
            R.string.nabla_conversation_composer_replying_to_title_author,
            repliedMessage.repliedToAuthorName(context),
        )
        currentlyReplyingToThumbnail.loadReplyContentThumbnailOrHide(repliedMessage.content)
    }

    private fun NablaFragmentConversationBinding.setMediaToSend(mediasToSend: List<LocalMedia>) {
        val mediasToSendAdapter = MediaToSendAdapter(
            onMediaClickedListener = {},
            onDeleteMediaToSendClickListener = {},
            onErrorLoadingVideoThumbnail = {},
        )

        conversationMediasToSendRecyclerView.apply {
            layoutManager = LinearLayoutManager(context.withNablaMessagingThemeOverlays(), LinearLayoutManager.HORIZONTAL, false)
            adapter = mediasToSendAdapter
        }

        mediasToSendAdapter.submitList(mediasToSend)

        conversationMediasToSendRecyclerView.visibility =
            if (mediasToSend.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun createBinding(context: Context, layoutInflater: LayoutInflater): Pair<View, NablaFragmentConversationBinding> {
        val parent = FrameLayout(context.withNablaMessagingThemeOverlays())

        val binding = NablaFragmentConversationBinding.inflate(layoutInflater.cloneInContext(context.withNablaMessagingThemeOverlays()), parent, false)
        parent.addView(binding.root)

        binding.nablaIncludedErrorLayout.root.isVisible = false
        binding.conversationLoaded.isVisible = true

        return Pair(parent, binding)
    }

    private fun makeRepliedProviderTextMessage(text: String) = makeRepliedProviderMessage(RepliedMessage.Content.Text(text))

    private fun makeRepliedProviderImageMessage() = makeRepliedProviderMessage(
        RepliedMessage.Content.Image(
            Uri("https://google.com"),
        ),
    )

    private fun makeRepliedProviderVideoMessage() = makeRepliedProviderMessage(
        RepliedMessage.Content.Video(
            Uri("https://google.com"),
        ),
    )

    private fun makeRepliedProviderDocumentMessage() = makeRepliedProviderMessage(
        RepliedMessage.Content.Document(
            uri = Uri("https://google.com"),
            thumbnailUri = Uri("https://google.com"),
        ),
    )

    private fun makeRepliedProviderAudioMessage() = makeRepliedProviderMessage(
        RepliedMessage.Content.Audio(
            Uri("https://google.com"),
        ),
    )

    private fun makeRepliedProviderMessage(content: RepliedMessage.Content) = RepliedMessage(
        id = MessageId.Remote(Uuid.randomUUID(), Uuid.randomUUID()),
        content = content,
        author = providerAuthor,
    )

    private companion object {
        const val longText = "This is a super long text with multiple sentences, that are long, and a smiley: \uD83D\uDE00 and an url: https://www.google/com and a phone number +44663929491 . Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."
        val mediasToSend = listOf(
            LocalMedia.Image(
                uri = URI.create("https://google.com/"),
                name = null,
                mimeType = MimeType.Image.Jpeg,
            ),
            LocalMedia.Video(
                uri = URI.create("https://google.com/"),
                name = null,
                mimeType = MimeType.Video.Mp4,
            ),
            LocalMedia.Document(
                uri = URI.create("https://google.com/"),
                name = null,
                mimeType = MimeType.Application.Pdf,
            ),
        )
        val providerAuthor = TimelineItem.Message.Author.Provider(
            provider = Provider.fake(
                firstName = "Mario",
                lastName = "Bros",
            ),
        )
    }
}
