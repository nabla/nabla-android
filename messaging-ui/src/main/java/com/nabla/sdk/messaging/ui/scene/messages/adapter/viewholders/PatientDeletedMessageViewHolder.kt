package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemPatientMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.DeletedMessageContentBinder

internal class PatientDeletedMessageViewHolder(
    binding: NablaConversationTimelineItemPatientMessageBinding,
    contentBinder: DeletedMessageContentBinder,
) : PatientMessageViewHolder<TimelineItem.Message.Deleted, DeletedMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(inflater: LayoutInflater, parent: ViewGroup): PatientDeletedMessageViewHolder {
            val binding = NablaConversationTimelineItemPatientMessageBinding.inflate(inflater, parent, false)
            return PatientDeletedMessageViewHolder(
                binding,
                DeletedMessageContentBinder
                    .create(R.attr.nablaMessaging_conversationDeletedMessageAppearance, inflater, binding.chatPatientMessageContentContainer)
            )
        }
    }
}
