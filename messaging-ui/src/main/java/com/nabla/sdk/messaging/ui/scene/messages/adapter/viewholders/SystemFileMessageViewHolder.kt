package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemSystemMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.FileMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateSystemMessageContentCard

internal class SystemFileMessageViewHolder(
    binding: NablaConversationTimelineItemSystemMessageBinding,
    contentBinder: FileMessageContentBinder,
) : SystemMessageViewHolder<TimelineItem.Message.File, FileMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(inflater: LayoutInflater, parent: ViewGroup): SystemFileMessageViewHolder {
            val binding = NablaConversationTimelineItemSystemMessageBinding.inflate(inflater, parent, false)
            return SystemFileMessageViewHolder(
                binding,
                inflateSystemMessageContentCard(inflater, binding.chatSystemMessageContentContainer) { content ->
                    FileMessageContentBinder.create(R.attr.colorPrimaryDark, R.attr.colorSurface, inflater, content)
                }
            )
        }
    }
}
