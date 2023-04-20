package com.nabla.sdk.messaging

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benasher44.uuid.uuid4
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.BindingPayload
import com.nabla.sdk.messaging.ui.scene.messages.adapter.ConversationDiffCallback
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class ConversationDiffCallbackTest {

    @Test
    fun `Minor change to Date item of same Message is ignored`() {
        val oldItem = TimelineItem.DateSeparator(
            date = Clock.System.now(),
            listItemId = uuid4().toString(),
        )
        val newItem = oldItem.copy(date = oldItem.date.plus(200.milliseconds))

        assertEquals(true, ConversationDiffCallback.areItemsTheSame(oldItem, newItem))
        assertEquals(true, ConversationDiffCallback.areContentsTheSame(oldItem, newItem))
    }

    @Test
    fun `Change in remote uuid only triggers Callbacks payload`() {
        val oldId = MessageId.Local(uuid4())
        val newId = MessageId.Remote(clientId = oldId.clientId, remoteId = uuid4())

        val oldItem = TimelineItem.Message.fake(id = oldId, content = TimelineItem.Message.Text.fake())
        val newItem = oldItem.copy(id = newId)

        assertEquals(true, ConversationDiffCallback.areItemsTheSame(oldItem, newItem))
        assertEquals(false, ConversationDiffCallback.areContentsTheSame(oldItem, newItem))
        assertEquals(
            BindingPayload.Callbacks(
                actions = newItem.actions,
                itemForCallback = newItem,
            ),
            ConversationDiffCallback.getChangePayload(oldItem, newItem),
        )
    }

    @Test
    fun `Change in status only triggers PatientMessageStatus payload`() {
        val oldItem = TimelineItem.Message.fake(
            showStatus = true,
            content = TimelineItem.Message.Text.fake(),
        )
        val newItem = oldItem.copy(showStatus = false)

        assertEquals(true, ConversationDiffCallback.areItemsTheSame(oldItem, newItem))
        assertEquals(false, ConversationDiffCallback.areContentsTheSame(oldItem, newItem))
        assertEquals(
            BindingPayload.PatientSendStatus(
                status = newItem.status,
                showStatus = newItem.showStatus,
                actions = newItem.actions,
                itemForCallback = newItem,
            ),
            ConversationDiffCallback.getChangePayload(oldItem, newItem),
        )
    }

    @Test
    fun `Successful message sending only triggers PatientMessageStatus payload`() {
        val oldItem = TimelineItem.Message.fake(
            sendStatus = SendStatus.Sending,
            content = TimelineItem.Message.Text.fake(),
        )
        val newItem = oldItem.copy(
            status = SendStatus.Sent,
        )

        assertEquals(true, ConversationDiffCallback.areItemsTheSame(oldItem, newItem))
        assertEquals(false, ConversationDiffCallback.areContentsTheSame(oldItem, newItem))
        assertEquals(
            BindingPayload.PatientSendStatus(
                status = newItem.status,
                showStatus = newItem.showStatus,
                actions = newItem.actions,
                itemForCallback = newItem,
            ),
            ConversationDiffCallback.getChangePayload(oldItem, newItem),
        )
    }
}
