package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemOtherMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.ImageMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateOtherMessageContentCard

internal class OtherImageMessageViewHolder(
    binding: NablaConversationTimelineItemOtherMessageBinding,
    contentBinder: ImageMessageContentBinder,
) : OtherMessageViewHolder<TimelineItem.Message.Image, ImageMessageContentBinder>(binding, contentBinder) {

    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
        ): OtherImageMessageViewHolder {
            val binding = NablaConversationTimelineItemOtherMessageBinding.inflate(inflater, parent, false)
            return OtherImageMessageViewHolder(
                binding,
                inflateOtherMessageContentCard(inflater, binding.chatOtherMessageContentContainer) { contentParent ->
                    ImageMessageContentBinder.create(R.attr.nablaMessaging_conversationOtherMessageAppearance, inflater, contentParent)
                },
            )
        }
    }
}
