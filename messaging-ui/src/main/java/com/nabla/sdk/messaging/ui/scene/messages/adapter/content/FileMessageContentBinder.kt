package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import coil.load
import com.google.android.material.resources.TextAppearance
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.getThemeColor
import com.nabla.sdk.core.ui.helpers.getThemeStyle
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemFileMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

@SuppressLint("RestrictedApi") // TextAppearance class
internal class FileMessageContentBinder(
    @AttrRes contentTextAppearanceAttr: Int,
    @AttrRes surfaceColorAttr: Int,
    private val binding: NablaConversationTimelineItemFileMessageBinding,
) : MessageContentBinder<TimelineItem.Message.File>() {

    private val contentAppearance = TextAppearance(binding.context, binding.context.getThemeStyle(contentTextAppearanceAttr))

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
        binding.chatFileMessageIconImageView.imageTintList = contentAppearance.textColor
        binding.chatFileMessageTitleTextView.setTextColor(contentAppearance.textColor)
        binding.chatFileMessageTitleTextView.text = item.fileName
    }

    companion object {
        fun create(
            @AttrRes contentTextAppearanceAttr: Int,
            @AttrRes surfaceColorAttr: Int,
            inflater: LayoutInflater,
            parent: ViewGroup,
        ): FileMessageContentBinder {
            return FileMessageContentBinder(
                contentTextAppearanceAttr = contentTextAppearanceAttr,
                surfaceColorAttr = surfaceColorAttr,
                binding = NablaConversationTimelineItemFileMessageBinding.inflate(inflater, parent, true)
            )
        }
    }
}
