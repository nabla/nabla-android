package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.AttrRes
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeStyle
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemDeletedMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal class DeletedMessageContentBinder(
    @AttrRes contentTextAppearanceAttr: Int,
    binding: NablaConversationTimelineItemDeletedMessageBinding,
) : MessageContentBinder<TimelineItem.Message.Deleted>() {

    init {
        binding.chatDeletedMessageTextView.setTextAppearance(binding.context.getThemeStyle(contentTextAppearanceAttr))
    }

    override fun bind(messageId: String, item: TimelineItem.Message.Deleted) {
        /* no-op */
    }

    companion object {
        fun create(
            @AttrRes contentTextAppearanceAttr: Int,
            inflater: LayoutInflater,
            parent: ViewGroup,
        ): DeletedMessageContentBinder {
            return DeletedMessageContentBinder(
                contentTextAppearanceAttr = contentTextAppearanceAttr,
                binding = NablaConversationTimelineItemDeletedMessageBinding.inflate(inflater, parent, true),
            )
        }
    }
}
