package com.nabla.sdk.messaging.ui.messages

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.android.ide.common.rendering.api.SessionParams
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.fake
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays
import com.nabla.sdk.messaging.ui.scene.messages.PlaybackProgress
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.MessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherAudioMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherFileMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherImageMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherVideoMessageViewHolder
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.temporal.ChronoUnit

internal class ConversationOtherMediaCellTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule(
        renderingMode = SessionParams.RenderingMode.V_SCROLL,
    )

    @Test
    fun `test other image`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val parent = FrameLayout(context.withNablaMessagingThemeOverlays())
        parent.setPadding(0, 50, 0, 50)
        val viewHolder = OtherImageMessageViewHolder.create(
            inflater = layoutInflater.cloneInContext(context.withNablaMessagingThemeOverlays()),
            parent = parent,
        )

        parent.addView(viewHolder.itemView)

        val content = TimelineItem.Message.Image(
            uri = Uri("https://google.com/"),
        )

        viewHolder.bindContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test other video`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val parent = FrameLayout(context.withNablaMessagingThemeOverlays())
        parent.setPadding(0, 50, 0, 50)
        val viewHolder = OtherVideoMessageViewHolder.create(
            inflater = layoutInflater.cloneInContext(context.withNablaMessagingThemeOverlays()),
            parent = parent,
            onErrorFetchingVideoThumbnail = {},
        )

        parent.addView(viewHolder.itemView)

        val content = TimelineItem.Message.Video(
            uri = Uri("https://google.com/"),
        )

        viewHolder.bindContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test other document with thumbnail`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val parent = FrameLayout(context.withNablaMessagingThemeOverlays())
        parent.setPadding(0, 50, 0, 50)
        val viewHolder = OtherFileMessageViewHolder.create(
            inflater = layoutInflater.cloneInContext(context.withNablaMessagingThemeOverlays()),
            parent = parent,
        )

        parent.addView(viewHolder.itemView)

        val content = TimelineItem.Message.File(
            uri = Uri("https://google.com/"),
            fileName = "file.pdf",
            mimeType = MimeType.Application.Pdf,
            thumbnailUri = Uri("https://google.com/"),
        )

        viewHolder.bindContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test other document without thumbnail`() = paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
        val parent = FrameLayout(context.withNablaMessagingThemeOverlays())
        parent.setPadding(0, 50, 0, 50)
        val viewHolder = OtherFileMessageViewHolder.create(
            inflater = layoutInflater.cloneInContext(context.withNablaMessagingThemeOverlays()),
            parent = parent,
        )

        parent.addView(viewHolder.itemView)

        val content = TimelineItem.Message.File(
            uri = Uri("https://google.com/"),
            fileName = "file.pdf",
            mimeType = MimeType.Application.Pdf,
            thumbnailUri = null,
        )

        viewHolder.bindContent(content)

        return@snapshotDayNightDefaultDevice parent
    }

    @Test
    fun `test other audio not playing at 0 percent`() = paparazzi.snapshotDayNightDefaultDevice(offsetMillis = 250) { (context, layoutInflater) ->
        createAudioMessageMessageView(
            context = context,
            layoutInflater = layoutInflater,
            playing = false,
            progressPercent = 0,
        )
    }

    @Test
    fun `test other audio not playing at 50 percent`() = paparazzi.snapshotDayNightDefaultDevice(offsetMillis = 250) { (context, layoutInflater) ->
        createAudioMessageMessageView(
            context = context,
            layoutInflater = layoutInflater,
            playing = false,
            progressPercent = 50,
        )
    }

    @Test
    fun `test other audio not playing at 100 percent`() = paparazzi.snapshotDayNightDefaultDevice(offsetMillis = 250) { (context, layoutInflater) ->
        createAudioMessageMessageView(
            context = context,
            layoutInflater = layoutInflater,
            playing = false,
            progressPercent = 100,
        )
    }

    @Test
    fun `test other audio playing at 0 percent`() = paparazzi.snapshotDayNightDefaultDevice(offsetMillis = 250) { (context, layoutInflater) ->
        createAudioMessageMessageView(
            context = context,
            layoutInflater = layoutInflater,
            playing = true,
            progressPercent = 0,
        )
    }

    @Test
    fun `test other audio playing at 50 percent`() = paparazzi.snapshotDayNightDefaultDevice(offsetMillis = 250) { (context, layoutInflater) ->
        createAudioMessageMessageView(
            context = context,
            layoutInflater = layoutInflater,
            playing = true,
            progressPercent = 50,
        )
    }

    @Test
    fun `test other audio playing at 100 percent`() = paparazzi.snapshotDayNightDefaultDevice(offsetMillis = 250) { (context, layoutInflater) ->
        createAudioMessageMessageView(
            context = context,
            layoutInflater = layoutInflater,
            playing = true,
            progressPercent = 100,
        )
    }

    private fun createAudioMessageMessageView(
        context: Context,
        layoutInflater: LayoutInflater,
        playing: Boolean,
        @android.annotation.IntRange(from = 0, to = 100) progressPercent: Long,
    ): View {
        val parent = FrameLayout(context.withNablaMessagingThemeOverlays())
        parent.setPadding(0, 50, 0, 50)
        val viewHolder = OtherAudioMessageViewHolder.create(
            inflater = layoutInflater.cloneInContext(context.withNablaMessagingThemeOverlays()),
            parent = parent,
            onToggleAudioMessagePlay = {},
        )

        parent.addView(viewHolder.itemView)

        val content = TimelineItem.Message.Audio(
            uri = Uri("https://google.com/"),
            progress = PlaybackProgress(currentPositionMillis = ((progressPercent / 100.0) * audioDuration.toMillis()).toLong(), totalDurationMillis = audioDuration.toMillis()),
            isPlaying = playing,
        )

        viewHolder.bindContent(content)

        return parent
    }

    private fun <C : TimelineItem.Message.Content, B : MessageContentBinder<C>> OtherMessageViewHolder<C, B>.bindContent(content: C) {
        bind(
            TimelineItem.Message.fake(
                sendStatus = SendStatus.Sent,
                showStatus = true,
                time = Instant.DISTANT_PAST,
                content = content,
                showAuthorName = true,
                showAuthorAvatar = true,
            ),
            author,
            content,
        )
    }

    private companion object {
        private val audioDuration = Duration.of(3, ChronoUnit.MINUTES).plus(Duration.of(32, ChronoUnit.SECONDS))
        val author = TimelineItem.Message.Author.Other(
            uuid = Uuid.randomUUID(),
            displayName = "Luigi",
            avatar = null,
        )
    }
}
