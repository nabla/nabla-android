package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.benasher44.uuid.Uuid
import com.google.android.material.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.TextMessageContentBinder

internal class ProviderTextMessageViewHolder(
    binding: NablaConversationTimelineItemProviderMessageBinding,
    onProviderClicked: (providerId: Uuid) -> Unit,
    contentBinder: TextMessageContentBinder,
) : ProviderMessageViewHolder<TimelineItem.Message.Text, TextMessageContentBinder>(binding, onProviderClicked, contentBinder) {
    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onProviderClicked: (providerId: Uuid) -> Unit,
            onUrlClicked: (url: String) -> Unit,
        ): ProviderTextMessageViewHolder {
            val binding = NablaConversationTimelineItemProviderMessageBinding.inflate(inflater, parent, false)
            return ProviderTextMessageViewHolder(
                binding,
                onProviderClicked,
                inflateProviderMessageContentCard(inflater, binding.chatProviderMessageContentContainer) { content ->
                    TextMessageContentBinder.create(R.attr.colorPrimaryDark, inflater, content, onUrlClicked)
                }
            )
        }
    }
}
