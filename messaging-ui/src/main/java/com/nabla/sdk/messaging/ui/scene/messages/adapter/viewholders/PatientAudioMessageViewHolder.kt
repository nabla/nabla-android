package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemPatientMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.AudioMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflatePatientMessageContentCard

internal class PatientAudioMessageViewHolder(
    binding: NablaConversationTimelineItemPatientMessageBinding,
    contentBinder: AudioMessageContentBinder,
) : PatientMessageViewHolder<TimelineItem.Message.Audio, AudioMessageContentBinder>(binding, contentBinder) {

    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onToggleAudioMessagePlay: (audioMessageUri: Uri) -> Unit,
        ): PatientAudioMessageViewHolder {
            val binding = NablaConversationTimelineItemPatientMessageBinding.inflate(inflater, parent, false)
            return PatientAudioMessageViewHolder(
                binding,
                inflatePatientMessageContentCard(inflater, binding.chatPatientMessageContentContainer) { contentParent ->
                    AudioMessageContentBinder.create(
                        contentTextAppearanceAttr = R.attr.nablaMessaging_conversationPatientMessageAppearance,
                        surfaceColorAttr = R.attr.nablaMessaging_patientMessageBackgroundColor,
                        progressBackgroundDrawableRes = R.drawable.nabla_patient_audio_message_progress_bar,
                        inflater = inflater,
                        parent = contentParent,
                        onToggleAudioMessagePlay = onToggleAudioMessagePlay,
                    )
                },
            )
        }
    }
}
