package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemOtherMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.VideoMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateOtherMessageContentCard

internal class OtherVideoMessageViewHolder(
    binding: NablaConversationTimelineItemOtherMessageBinding,
    contentBinder: VideoMessageContentBinder,
) : OtherMessageViewHolder<TimelineItem.Message.Video, VideoMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onErrorFetchingVideoThumbnail: (error: Throwable) -> Unit,
        ): OtherVideoMessageViewHolder {
            val binding = NablaConversationTimelineItemOtherMessageBinding.inflate(inflater, parent, false)
            return OtherVideoMessageViewHolder(
                binding,
                inflateOtherMessageContentCard(inflater, binding.chatOtherMessageContentContainer) { contentParent ->
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
