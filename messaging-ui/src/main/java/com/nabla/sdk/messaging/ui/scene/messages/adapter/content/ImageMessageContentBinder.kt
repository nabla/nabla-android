package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import coil.load
import coil.size.Size
import com.google.android.material.resources.TextAppearance
import com.nabla.sdk.core.data.helper.UrlExt.toAndroidUri
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeStyle
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemImageMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

@SuppressLint("RestrictedApi") // TextAppearance class
internal class ImageMessageContentBinder(
    @AttrRes private val contentTextAppearanceAttr: Int,
    private val binding: NablaConversationTimelineItemImageMessageBinding,
) : MessageContentBinder<TimelineItem.Message.Image>() {
    private val contentAppearance = TextAppearance(binding.context, binding.context.getThemeStyle(contentTextAppearanceAttr))

    override fun bind(messageId: String, item: TimelineItem.Message.Image) {
        binding.chatImageMessageLoadingProgressBar.visibility = View.VISIBLE
        binding.chatImageMessageLoadingProgressBar.indeterminateTintList = contentAppearance.textColor

        loadImage(uri = item.uri.toAndroidUri(), itemId = messageId)
    }

    fun loadImage(uri: Uri, itemId: String) {
        binding.chatImageMessageImageView.load(uri) {
            size(Size(binding.context.resources.displayMetrics.widthPixels, binding.chatImageMessageImageView.maxHeight))
            memoryCacheKey(itemId)
            placeholderMemoryCacheKey(itemId)
            listener(onSuccess = { _, _ -> binding.chatImageMessageLoadingProgressBar.visibility = View.GONE })
        }
    }

    companion object {
        fun create(
            @AttrRes contentTextAppearanceAttr: Int,
            inflater: LayoutInflater,
            parent: ViewGroup,
        ): ImageMessageContentBinder {
            return ImageMessageContentBinder(
                contentTextAppearanceAttr = contentTextAppearanceAttr,
                binding = NablaConversationTimelineItemImageMessageBinding.inflate(inflater, parent, true)
            )
        }
    }
}
