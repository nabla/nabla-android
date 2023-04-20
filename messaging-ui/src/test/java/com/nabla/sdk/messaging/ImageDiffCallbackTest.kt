package com.nabla.sdk.messaging

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nabla.sdk.core.data.helper.UrlExt.toAndroidUri
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.BindingPayload
import com.nabla.sdk.messaging.ui.scene.messages.adapter.ConversationDiffCallback
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ImageDiffCallbackTest {

    @Test
    fun `Change in image uri only triggers Image payload`() {
        val oldItem = TimelineItem.Message.fake(content = TimelineItem.Message.Image.fake())
        val newItem = oldItem.copy(
            content = (oldItem.content as TimelineItem.Message.Image).copy(
                uri = Uri("${oldItem.content.uri}_new"),
            ),
        )

        assertEquals(true, ConversationDiffCallback.areItemsTheSame(oldItem, newItem))
        assertEquals(false, ConversationDiffCallback.areContentsTheSame(oldItem, newItem))
        assertEquals(
            BindingPayload.Image(
                uri = (newItem.content as TimelineItem.Message.Image).uri.toAndroidUri(),
                itemId = newItem.listItemId,
                status = newItem.status,
                showStatus = newItem.showStatus,
                actions = newItem.actions,
                itemForCallback = newItem,
            ),
            ConversationDiffCallback.getChangePayload(oldItem, newItem),
        )
    }
}
