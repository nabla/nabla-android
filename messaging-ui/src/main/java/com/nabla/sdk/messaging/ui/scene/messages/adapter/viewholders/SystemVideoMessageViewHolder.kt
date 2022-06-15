package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemSystemMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.VideoMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateSystemMessageContentCard

internal class SystemVideoMessageViewHolder(
    binding: NablaConversationTimelineItemSystemMessageBinding,
    contentBinder: VideoMessageContentBinder,
) : SystemMessageViewHolder<TimelineItem.Message.Video, VideoMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onErrorFetchingVideoThumbnail: (error: Throwable) -> Unit,
        ): SystemVideoMessageViewHolder {
            val binding = NablaConversationTimelineItemSystemMessageBinding.inflate(inflater, parent, false)
            return SystemVideoMessageViewHolder(
                binding,
                inflateSystemMessageContentCard(inflater, binding.chatSystemMessageContentContainer) { contentParent ->
                    VideoMessageContentBinder.create(
                        contentTextAppearanceAttr = R.attr.nablaMessaging_conversationProviderMessageAppearance,
                        inflater = inflater,
                        parent = contentParent,
                        onErrorFetchingVideoThumbnail = onErrorFetchingVideoThumbnail,
                    )
                }
            )
        }
    }
}
