package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.benasher44.uuid.Uuid
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemProviderMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.LivekitRoomMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateProviderMessageContentCard

internal class ProviderLivekitRoomMessageViewHolder(
    binding: NablaConversationTimelineItemProviderMessageBinding,
    onProviderClicked: (providerId: Uuid) -> Unit,
    contentBinder: LivekitRoomMessageContentBinder,
) : ProviderMessageViewHolder<TimelineItem.Message.LivekitRoom, LivekitRoomMessageContentBinder>(binding, onProviderClicked, contentBinder) {

    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onProviderClicked: (providerId: Uuid) -> Unit,
            onJoinLivekitRoomClicked: (String, String, String) -> Unit,
        ): ProviderLivekitRoomMessageViewHolder {
            val binding = NablaConversationTimelineItemProviderMessageBinding.inflate(
                inflater,
                parent,
                false,
            )
            return ProviderLivekitRoomMessageViewHolder(
                binding,
                onProviderClicked,
                inflateProviderMessageContentCard(inflater, binding.chatProviderMessageContentContainer) { contentParent ->
                    LivekitRoomMessageContentBinder.create(
                        contentTextAppearanceAttr = R.attr.nablaMessaging_conversationProviderMessageAppearance,
                        surfaceColorAttr = R.attr.nablaMessaging_providerMessageBackgroundColor,
                        inflater,
                        contentParent,
                        onJoinLivekitRoomClicked,
                    )
                },
            )
        }
    }
}
