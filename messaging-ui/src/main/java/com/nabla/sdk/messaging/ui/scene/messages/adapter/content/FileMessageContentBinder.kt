package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import coil.load
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.getThemeColor
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemFileMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal class FileMessageContentBinder(
    @AttrRes contentColorAttr: Int,
    @AttrRes surfaceColorAttr: Int,
    private val binding: NablaConversationTimelineItemFileMessageBinding,
) : MessageContentBinder<TimelineItem.Message.File>(contentColorAttr) {

    @ColorInt
    private val contentColor: Int = binding.context.getThemeColor(contentColorAttr)

    @ColorInt
    private val surfaceColor: Int = binding.context.getThemeColor(surfaceColorAttr)

    override fun bind(messageId: String, item: TimelineItem.Message.File) {
        if (item.thumbnailUri != null) {
            binding.chatFileMessagePreviewImageView.load(item.thumbnailUri.toAndroidUri()) {
                placeholder(R.drawable.nabla_file_placeholder)
            }
        } else {
            binding.chatFileMessagePreviewImageView.setImageResource(R.drawable.nabla_file_placeholder)
        }
        binding.chatFileMessageTitleContainer.setBackgroundColor(surfaceColor)
        binding.chatFileMessageIconImageView.imageTintList = ColorStateList.valueOf(contentColor)
        binding.chatFileMessageTitleTextView.setTextColor(contentColor)
        binding.chatFileMessageTitleTextView.text = item.fileName
    }

    companion object {
        fun create(
            @AttrRes contentColorAttr: Int,
            @AttrRes surfaceColorAttr: Int,
            inflater: LayoutInflater,
            parent: ViewGroup,
        ): FileMessageContentBinder {
            return FileMessageContentBinder(
                contentColorAttr = contentColorAttr,
                surfaceColorAttr = surfaceColorAttr,
                binding = NablaConversationTimelineItemFileMessageBinding.inflate(inflater, parent, true)
            )
        }
    }
}
