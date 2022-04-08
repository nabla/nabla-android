package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemSystemMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.ImageMessageContentBinder
import com.google.android.material.R as MaterialR

internal class SystemImageMessageViewHolder(
    binding: NablaConversationTimelineItemSystemMessageBinding,
    contentBinder: ImageMessageContentBinder,
) : SystemMessageViewHolder<TimelineItem.Message.Image, ImageMessageContentBinder>(binding, contentBinder) {

    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
        ): SystemImageMessageViewHolder {
            val binding = NablaConversationTimelineItemSystemMessageBinding.inflate(inflater, parent, false)
            return SystemImageMessageViewHolder(
                binding,
                inflateSystemMessageContentCard(inflater, binding.chatSystemMessageContentContainer) { contentParent ->
                    ImageMessageContentBinder.create(MaterialR.attr.colorPrimaryDark, inflater, contentParent)
                }
            )
        }
    }
}
