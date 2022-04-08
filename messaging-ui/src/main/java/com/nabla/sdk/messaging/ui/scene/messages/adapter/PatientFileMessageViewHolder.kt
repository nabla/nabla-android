package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemPatientMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.FileMessageContentBinder

internal class PatientFileMessageViewHolder(
    binding: NablaConversationTimelineItemPatientMessageBinding,
    contentBinder: FileMessageContentBinder,
) : PatientMessageViewHolder<TimelineItem.Message.File, FileMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(inflater: LayoutInflater, parent: ViewGroup): PatientFileMessageViewHolder {
            val binding = NablaConversationTimelineItemPatientMessageBinding.inflate(inflater, parent, false)
            return PatientFileMessageViewHolder(
                binding,
                inflatePatientMessageContentCard(inflater, binding.chatPatientMessageContentContainer) { contentParent ->
                    FileMessageContentBinder.create(R.attr.colorOnPrimary, R.attr.colorPrimaryDark, inflater, contentParent)
                }
            )
        }
    }
}
