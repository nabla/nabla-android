package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.benasher44.uuid.Uuid
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.DeletedMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateProviderMessageContentCard

internal class ProviderDeletedMessageViewHolder(
    binding: NablaConversationTimelineItemProviderMessageBinding,
    onProviderClicked: (providerId: Uuid) -> Unit,
    contentBinder: DeletedMessageContentBinder,
) : ProviderMessageViewHolder<TimelineItem.Message.Deleted, DeletedMessageContentBinder>(binding, onProviderClicked, contentBinder) {
    companion object {
        fun create(inflater: LayoutInflater, parent: ViewGroup, onProviderClicked: (providerId: Uuid) -> Unit): ProviderDeletedMessageViewHolder {
            val binding = NablaConversationTimelineItemProviderMessageBinding.inflate(inflater, parent, false)
            return ProviderDeletedMessageViewHolder(
                binding,
                onProviderClicked,
                inflateProviderMessageContentCard(inflater, binding.chatProviderMessageContentContainer) { content ->
                    DeletedMessageContentBinder.create(R.attr.nablaMessaging_conversationDeletedMessageAppearance, inflater, content)
                }
            )
        }
    }
}
