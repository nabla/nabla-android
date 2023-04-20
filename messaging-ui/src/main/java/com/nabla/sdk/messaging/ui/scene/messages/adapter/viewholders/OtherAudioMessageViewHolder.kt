package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemOtherMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.AudioMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateOtherMessageContentCard

internal class OtherAudioMessageViewHolder(
    binding: NablaConversationTimelineItemOtherMessageBinding,
    contentBinder: AudioMessageContentBinder,
) : OtherMessageViewHolder<TimelineItem.Message.Audio, AudioMessageContentBinder>(binding, contentBinder) {

    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onToggleAudioMessagePlay: (audioMessageUri: Uri) -> Unit,
        ): OtherAudioMessageViewHolder {
            val binding = NablaConversationTimelineItemOtherMessageBinding.inflate(inflater, parent, false)
            return OtherAudioMessageViewHolder(
                binding,
                inflateOtherMessageContentCard(inflater, binding.chatOtherMessageContentContainer) { contentParent ->
                    AudioMessageContentBinder.create(
                        contentTextAppearanceAttr = R.attr.nablaMessaging_conversationOtherMessageAppearance,
                        surfaceColorAttr = R.attr.nablaMessaging_otherMessageBackgroundColor,
                        progressBackgroundDrawableRes = R.drawable.nabla_other_audio_message_progress_bar,
                        inflater = inflater,
                        parent = contentParent,
                        onToggleAudioMessagePlay = onToggleAudioMessagePlay,
                    )
                },
            )
        }
    }
}
