package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemPatientMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.TextMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflatePatientMessageContentCard

internal class PatientTextMessageViewHolder(
    binding: NablaConversationTimelineItemPatientMessageBinding,
    contentBinder: TextMessageContentBinder,
) : PatientMessageViewHolder<TimelineItem.Message.Text, TextMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onUrlClicked: (url: String) -> Unit,
            onRepliedMessageClicked: (MessageId) -> Unit,
        ): PatientTextMessageViewHolder {
            val binding = NablaConversationTimelineItemPatientMessageBinding.inflate(inflater, parent, false)
            return PatientTextMessageViewHolder(
                binding,
                inflatePatientMessageContentCard(inflater, binding.chatPatientMessageContentContainer) { contentParent ->
                    TextMessageContentBinder.create(
                        R.attr.nablaMessaging_conversationPatientMessageAppearance,
                        inflater,
                        contentParent,
                        onUrlClicked,
                        onRepliedMessageClicked,
                    )
                }
            )
        }
    }
}
