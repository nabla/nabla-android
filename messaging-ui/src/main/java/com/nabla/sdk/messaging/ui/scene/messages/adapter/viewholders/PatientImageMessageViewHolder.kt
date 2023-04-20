package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemPatientMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.ImageMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflatePatientMessageContentCard

internal class PatientImageMessageViewHolder(
    binding: NablaConversationTimelineItemPatientMessageBinding,
    contentBinder: ImageMessageContentBinder,
) : PatientMessageViewHolder<TimelineItem.Message.Image, ImageMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(inflater: LayoutInflater, parent: ViewGroup): PatientImageMessageViewHolder {
            val binding = NablaConversationTimelineItemPatientMessageBinding.inflate(inflater, parent, false)
            return PatientImageMessageViewHolder(
                binding,
                inflatePatientMessageContentCard(inflater, binding.chatPatientMessageContentContainer) { contentParent ->
                    ImageMessageContentBinder.create(R.attr.nablaMessaging_conversationPatientMessageAppearance, inflater, contentParent)
                },
            )
        }
    }
}
