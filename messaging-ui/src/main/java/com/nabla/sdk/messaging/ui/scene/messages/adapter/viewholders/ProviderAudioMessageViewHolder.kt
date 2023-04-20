package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.AudioMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateProviderMessageContentCard

internal class ProviderAudioMessageViewHolder(
    binding: NablaConversationTimelineItemProviderMessageBinding,
    onProviderClicked: (providerId: Uuid) -> Unit,
    contentBinder: AudioMessageContentBinder,
) : ProviderMessageViewHolder<TimelineItem.Message.Audio, AudioMessageContentBinder>(binding, onProviderClicked, contentBinder) {

    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onProviderClicked: (providerId: Uuid) -> Unit,
            onToggleAudioMessagePlay: (audioMessageUri: Uri) -> Unit,
        ): ProviderAudioMessageViewHolder {
            val binding = NablaConversationTimelineItemProviderMessageBinding.inflate(inflater, parent, false)
            return ProviderAudioMessageViewHolder(
                binding,
                onProviderClicked,
                inflateProviderMessageContentCard(inflater, binding.chatProviderMessageContentContainer) { contentParent ->
                    AudioMessageContentBinder.create(
                        contentTextAppearanceAttr = R.attr.nablaMessaging_conversationProviderMessageAppearance,
                        surfaceColorAttr = R.attr.nablaMessaging_providerMessageBackgroundColor,
                        progressBackgroundDrawableRes = R.drawable.nabla_provider_audio_message_progress_bar,
                        inflater = inflater,
                        parent = contentParent,
                        onToggleAudioMessagePlay = onToggleAudioMessagePlay,
                    )
                },
            )
        }
    }
}
