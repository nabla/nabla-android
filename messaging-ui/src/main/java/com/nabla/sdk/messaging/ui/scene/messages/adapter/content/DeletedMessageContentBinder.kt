package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.AttrRes
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemDeletedMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal class DeletedMessageContentBinder(
    @AttrRes contentColorAttr: Int,
    @Suppress("UNUSED_PARAMETER") binding: NablaConversationTimelineItemDeletedMessageBinding,
) : MessageContentBinder<TimelineItem.Message.Deleted>(contentColorAttr) {

    override fun bind(messageId: String, item: TimelineItem.Message.Deleted) {
        /* no-op */
    }

    companion object {
        fun create(
            @AttrRes contentColorAttr: Int,
            inflater: LayoutInflater,
            parent: ViewGroup,
        ): DeletedMessageContentBinder {
            return DeletedMessageContentBinder(
                contentColorAttr = contentColorAttr,
                binding = NablaConversationTimelineItemDeletedMessageBinding.inflate(inflater, parent, true)
            )
        }
    }
}
