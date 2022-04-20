package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.benasher44.uuid.Uuid
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.FileMessageContentBinder

internal class ProviderFileMessageViewHolder(
    binding: NablaConversationTimelineItemProviderMessageBinding,
    onProviderClicked: (providerId: Uuid) -> Unit,
    contentBinder: FileMessageContentBinder,
) : ProviderMessageViewHolder<TimelineItem.Message.File, FileMessageContentBinder>(binding, onProviderClicked, contentBinder) {
    companion object {
        fun create(inflater: LayoutInflater, parent: ViewGroup, onProviderClicked: (providerId: Uuid) -> Unit): ProviderFileMessageViewHolder {
            val binding = NablaConversationTimelineItemProviderMessageBinding.inflate(inflater, parent, false)
            return ProviderFileMessageViewHolder(
                binding,
                onProviderClicked,
                inflateProviderMessageContentCard(inflater, binding.chatProviderMessageContentContainer) { content ->
                    FileMessageContentBinder.create(
                        R.attr.nablaMessaging_conversationProviderMessageAppearance,
                        R.attr.nablaMessaging_providerMessageBackgroundColor,
                        inflater,
                        content
                    )
                }
            )
        }
    }
}
