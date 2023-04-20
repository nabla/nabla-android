package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemPatientMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.VideoMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflatePatientMessageContentCard

internal class PatientVideoMessageViewHolder(
    binding: NablaConversationTimelineItemPatientMessageBinding,
    contentBinder: VideoMessageContentBinder,
) : PatientMessageViewHolder<TimelineItem.Message.Video, VideoMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onErrorFetchingVideoThumbnail: (error: Throwable) -> Unit,
        ): PatientVideoMessageViewHolder {
            val binding = NablaConversationTimelineItemPatientMessageBinding.inflate(inflater, parent, false)
            return PatientVideoMessageViewHolder(
                binding,
                inflatePatientMessageContentCard(inflater, binding.chatPatientMessageContentContainer) { contentParent ->
                    VideoMessageContentBinder.create(
                        contentTextAppearanceAttr = R.attr.nablaMessaging_conversationPatientMessageAppearance,
                        inflater = inflater,
                        parent = contentParent,
                        onErrorFetchingVideoThumbnail = onErrorFetchingVideoThumbnail,
                    )
                },
            )
        }
    }
}
