package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemSystemMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.TextMessageContentBinder

internal class SystemTextMessageViewHolder(
    binding: NablaConversationTimelineItemSystemMessageBinding,
    contentBinder: TextMessageContentBinder,
) : SystemMessageViewHolder<TimelineItem.Message.Text, TextMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onUrlClicked: (url: String) -> Unit,
        ): SystemTextMessageViewHolder {
            val binding = NablaConversationTimelineItemSystemMessageBinding.inflate(inflater, parent, false)
            return SystemTextMessageViewHolder(
                binding,
                inflateSystemMessageContentCard(inflater, binding.chatSystemMessageContentContainer) { contentParent ->
                    TextMessageContentBinder.create(R.attr.colorPrimaryDark, inflater, contentParent, onUrlClicked)
                }
            )
        }
    }
}
