package com.nabla.sdk.messaging

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nabla.sdk.core.data.stubs.UriFaker
import com.nabla.sdk.messaging.ui.scene.messages.PlaybackProgress
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.BindingPayload
import com.nabla.sdk.messaging.ui.scene.messages.adapter.ConversationDiffCallback
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AudioDiffCallbackTest {

    @Test
    fun `Change in audio uri only triggers Audio payload`() {
        val oldItem = TimelineItem.Message.fake(content = TimelineItem.Message.Audio.fake())
        val newItem = oldItem.copy(
            content = (oldItem.content as TimelineItem.Message.Audio).copy(
                uri = UriFaker.remote(),
            )
        )

        assertEquals(true, ConversationDiffCallback.areItemsTheSame(oldItem, newItem))
        assertEquals(false, ConversationDiffCallback.areContentsTheSame(oldItem, newItem))
        assertEquals(
            BindingPayload.Audio(
                uri = (newItem.content as TimelineItem.Message.Audio).uri,
                progress = newItem.content.progress,
                isPlaying = newItem.content.isPlaying,
                status = newItem.status,
                showStatus = newItem.showStatus,
                actions = newItem.actions,
                itemForCallback = newItem,
            ),
            ConversationDiffCallback.getChangePayload(oldItem, newItem)
        )
    }

    @Test
    fun `Change in audio progress only triggers Audio payload`() {
        val oldItem = TimelineItem.Message.fake(content = TimelineItem.Message.Audio.fake())
        val newItem = oldItem.copy(
            content = (oldItem.content as TimelineItem.Message.Audio).copy(
                progress = PlaybackProgress(currentPositionMillis = 1_000, totalDurationMillis = 5_000),
                isPlaying = true,
            )
        )

        assertEquals(true, ConversationDiffCallback.areItemsTheSame(oldItem, newItem))
        assertEquals(false, ConversationDiffCallback.areContentsTheSame(oldItem, newItem))
        assertEquals(
            BindingPayload.Audio(
                uri = (newItem.content as TimelineItem.Message.Audio).uri,
                progress = newItem.content.progress,
                isPlaying = newItem.content.isPlaying,
                status = newItem.status,
                showStatus = newItem.showStatus,
                actions = newItem.actions,
                itemForCallback = newItem,
            ),
            ConversationDiffCallback.getChangePayload(oldItem, newItem)
        )
    }
}
