package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.AttrRes
import coil.load
import com.google.android.material.resources.TextAppearance
import com.nabla.sdk.core.data.helper.UrlExt.toAndroidUri
import com.nabla.sdk.core.data.helper.UrlExt.toJvmUri
import com.nabla.sdk.core.ui.helpers.ColorExtensions.setBackgroundColor
import com.nabla.sdk.core.ui.helpers.ColorIntOrStateList
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeColor
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeStyle
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.core.ui.helpers.coil.LocalPdfPreviewFetcher
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
    private val contentAppearanceRes: Int = binding.context.getThemeStyle(contentTextAppearanceAttr)

    private val surfaceColor: ColorIntOrStateList = binding.context.getThemeColor(surfaceColorAttr)

    override fun bind(messageId: String, item: TimelineItem.Message.File) {
        if (item.thumbnailUri != null) {
            binding.chatFileMessagePreviewImageView.load(item.thumbnailUri.toAndroidUri()) {
                memoryCacheKey(messageId)
                placeholderMemoryCacheKey(messageId)
                placeholder(R.drawable.nabla_file_placeholder)
            }
        } else {
            binding.chatFileMessagePreviewImageView.load(item) {
                memoryCacheKey(messageId)
                placeholderMemoryCacheKey(messageId)
                placeholder(R.drawable.nabla_file_placeholder)
                error(R.drawable.nabla_file_placeholder)

                fetcherFactory<TimelineItem.Message.File> { data, options, _ ->
                    LocalPdfPreviewFetcher(binding.context, data.uri.toJvmUri(), options)
                }
            }
        }
        binding.chatFileMessageTitleContainer.setBackgroundColor(surfaceColor)
        binding.chatFileMessageIconImageView.imageTintList = contentAppearance.textColor
        binding.chatFileMessageTitleTextView.setTextAppearance(contentAppearanceRes)
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
                binding = NablaConversationTimelineItemFileMessageBinding.inflate(inflater, parent, true),
            )
        }
    }
}
