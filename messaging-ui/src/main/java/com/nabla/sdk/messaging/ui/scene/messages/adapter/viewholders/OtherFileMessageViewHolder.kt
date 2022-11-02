package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemOtherMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.FileMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateOtherMessageContentCard

internal class OtherFileMessageViewHolder(
    binding: NablaConversationTimelineItemOtherMessageBinding,
    contentBinder: FileMessageContentBinder,
) : OtherMessageViewHolder<TimelineItem.Message.File, FileMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(inflater: LayoutInflater, parent: ViewGroup): OtherFileMessageViewHolder {
            val binding = NablaConversationTimelineItemOtherMessageBinding.inflate(inflater, parent, false)
            return OtherFileMessageViewHolder(
                binding,
                inflateOtherMessageContentCard(inflater, binding.chatOtherMessageContentContainer) { content ->
                    FileMessageContentBinder.create(
                        contentTextAppearanceAttr = R.attr.nablaMessaging_conversationOtherMessageAppearance,
                        surfaceColorAttr = R.attr.nablaMessaging_otherMessageBackgroundColor,
                        inflater,
                        content,
                    )
                }
            )
        }
    }
}
