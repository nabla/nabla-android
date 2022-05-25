package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.benasher44.uuid.Uuid
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.TextMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateProviderMessageContentCard

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
                    TextMessageContentBinder
                        .create(R.attr.nablaMessaging_conversationProviderMessageAppearance, inflater, content, onUrlClicked)
                }
            )
        }
    }
}
