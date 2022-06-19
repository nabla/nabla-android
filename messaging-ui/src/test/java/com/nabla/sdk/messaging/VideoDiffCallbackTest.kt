package com.nabla.sdk.messaging

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.BindingPayload
import com.nabla.sdk.messaging.ui.scene.messages.adapter.ConversationDiffCallback
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class VideoDiffCallbackTest {

    @Test
    fun `Change in video uri only triggers Video payload`() {
        val oldItem = TimelineItem.Message.fake(content = TimelineItem.Message.Video.fake())
        val newItem = oldItem.copy(
            content = (oldItem.content as TimelineItem.Message.Video).copy(
                uri = Uri("${oldItem.content.uri}_new"),
            )
        )

        assertEquals(true, ConversationDiffCallback.areItemsTheSame(oldItem, newItem))
        assertEquals(false, ConversationDiffCallback.areContentsTheSame(oldItem, newItem))
        assertEquals(
            BindingPayload.Video(
                uri = (newItem.content as TimelineItem.Message.Video).uri.toAndroidUri(),
                itemId = newItem.listItemId,
                status = newItem.status,
                showStatus = newItem.showStatus,
                actions = newItem.actions,
                itemForCallback = newItem,
            ),
            ConversationDiffCallback.getChangePayload(oldItem, newItem)
        )
    }
}
