package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.benasher44.uuid.Uuid
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.VideoMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateProviderMessageContentCard

internal class ProviderVideoMessageViewHolder(
    binding: NablaConversationTimelineItemProviderMessageBinding,
    onProviderClicked: (providerId: Uuid) -> Unit,
    contentBinder: VideoMessageContentBinder,
) : ProviderMessageViewHolder<TimelineItem.Message.Video, VideoMessageContentBinder>(binding, onProviderClicked, contentBinder) {
    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onProviderClicked: (providerId: Uuid) -> Unit,
            onErrorFetchingVideoThumbnail: (error: Throwable) -> Unit,
        ): ProviderVideoMessageViewHolder {
            val binding = NablaConversationTimelineItemProviderMessageBinding.inflate(inflater, parent, false)
            return ProviderVideoMessageViewHolder(
                binding,
                onProviderClicked,
                inflateProviderMessageContentCard(inflater, binding.chatProviderMessageContentContainer) { contentParent ->
                    VideoMessageContentBinder.create(
                        contentTextAppearanceAttr = R.attr.nablaMessaging_conversationProviderMessageAppearance,
                        inflater = inflater,
                        parent = contentParent,
                        onErrorFetchingVideoThumbnail = onErrorFetchingVideoThumbnail,
                    )
                },
            )
        }
    }
}
