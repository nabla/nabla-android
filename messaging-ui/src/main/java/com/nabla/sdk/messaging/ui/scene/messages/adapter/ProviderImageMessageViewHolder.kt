package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.benasher44.uuid.Uuid
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.ImageMessageContentBinder

internal class ProviderImageMessageViewHolder(
    binding: NablaConversationTimelineItemProviderMessageBinding,
    onProviderClicked: (providerId: Uuid) -> Unit,
    contentBinder: ImageMessageContentBinder,
) : ProviderMessageViewHolder<TimelineItem.Message.Image, ImageMessageContentBinder>(binding, onProviderClicked, contentBinder) {

    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onProviderClicked: (providerId: Uuid) -> Unit,
        ): ProviderImageMessageViewHolder {
            val binding = NablaConversationTimelineItemProviderMessageBinding.inflate(inflater, parent, false)
            return ProviderImageMessageViewHolder(
                binding,
                onProviderClicked,
                inflateProviderMessageContentCard(inflater, binding.chatProviderMessageContentContainer) { contentParent ->
                    ImageMessageContentBinder.create(R.attr.nablaMessaging_conversationProviderMessageAppearance, inflater, contentParent)
                }
            )
        }
    }
}
