package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemOtherMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.DeletedMessageContentBinder

internal class OtherDeletedMessageViewHolder(
    binding: NablaConversationTimelineItemOtherMessageBinding,
    contentBinder: DeletedMessageContentBinder,
) : OtherMessageViewHolder<TimelineItem.Message.Deleted, DeletedMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(inflater: LayoutInflater, parent: ViewGroup): OtherDeletedMessageViewHolder {
            val binding = NablaConversationTimelineItemOtherMessageBinding.inflate(inflater, parent, false)
            return OtherDeletedMessageViewHolder(
                binding,
                DeletedMessageContentBinder
                    .create(R.attr.nablaMessaging_conversationDeletedMessageAppearance, inflater, binding.chatOtherMessageContentContainer),
            )
        }
    }
}
