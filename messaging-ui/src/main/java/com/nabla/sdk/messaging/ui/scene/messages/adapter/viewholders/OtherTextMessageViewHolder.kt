package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemOtherMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.TextMessageContentBinder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.inflateOtherMessageContentCard

internal class OtherTextMessageViewHolder(
    binding: NablaConversationTimelineItemOtherMessageBinding,
    contentBinder: TextMessageContentBinder,
) : OtherMessageViewHolder<TimelineItem.Message.Text, TextMessageContentBinder>(binding, contentBinder) {
    companion object {
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            onUrlClicked: (url: String) -> Unit,
            onRepliedMessageClicked: (MessageId) -> Unit,
        ): OtherTextMessageViewHolder {
            val binding = NablaConversationTimelineItemOtherMessageBinding.inflate(inflater, parent, false)
            return OtherTextMessageViewHolder(
                binding,
                inflateOtherMessageContentCard(inflater, binding.chatOtherMessageContentContainer) { contentParent ->
                    TextMessageContentBinder.create(
                        R.attr.nablaMessaging_conversationOtherMessageAppearance,
                        inflater,
                        contentParent,
                        onUrlClicked,
                        onRepliedMessageClicked
                    )
                }
            )
        }
    }
}
