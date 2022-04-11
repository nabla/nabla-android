package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.net.Uri
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.ui.scene.messages.MessageAction
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

/**
 * All possible payloads for partial binding
 */
internal sealed class BindingPayload {
    abstract val itemForCallback: TimelineItem

    // in addition to specified partial binding, also reset actions callback
    interface ResetActions {
        val actions: Set<MessageAction>
        val itemForCallback: TimelineItem.Message
    }

    interface StatusVisibility {
        val showStatus: Boolean
    }

    data class PatientSendStatus(
        val status: SendStatus,
        override val showStatus: Boolean,
        override val actions: Set<MessageAction>,
        override val itemForCallback: TimelineItem.Message,
    ) : BindingPayload(), ResetActions, StatusVisibility

    data class Image(
        val itemId: String,
        val uri: Uri,
        val status: SendStatus,
        override val showStatus: Boolean,
        override val actions: Set<MessageAction>,
        override val itemForCallback: TimelineItem.Message,
    ) : BindingPayload(), ResetActions, StatusVisibility

    data class Callbacks(
        override val actions: Set<MessageAction>,
        override val itemForCallback: TimelineItem.Message,
    ) : BindingPayload(), ResetActions
}
