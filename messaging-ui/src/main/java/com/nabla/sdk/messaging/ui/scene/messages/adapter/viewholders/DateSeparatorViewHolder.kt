package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import com.nabla.sdk.core.ui.helpers.DateFormattingExtension.toFormattedRelativeWeekDayAndShortTimeString
import com.nabla.sdk.core.ui.helpers.DateParsingExtension.toJavaDate
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemDateBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal class DateSeparatorViewHolder(private val binding: NablaConversationTimelineItemDateBinding) : ChatViewHolder(binding.root) {
    fun bind(item: TimelineItem.DateSeparator) {
        binding.chatDateTextView.text = item.date.toJavaDate().toFormattedRelativeWeekDayAndShortTimeString(binding.context)
    }

    companion object {
        fun create(inflater: LayoutInflater, parent: ViewGroup) = DateSeparatorViewHolder(
            NablaConversationTimelineItemDateBinding.inflate(
                inflater,
                parent,
                false,
            ),
        )
    }
}
