package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import coil.load
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.getThemeColor
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemImageMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal class ImageMessageContentBinder(
    @AttrRes contentColorAttr: Int,
    private val binding: NablaConversationTimelineItemImageMessageBinding,
) : MessageContentBinder<TimelineItem.Message.Image>(contentColorAttr) {

    override fun bind(messageId: String, item: TimelineItem.Message.Image) {
        binding.chatImageMessageLoadingProgressBar.visibility = View.VISIBLE
        binding.chatImageMessageLoadingProgressBar.indeterminateTintList = ColorStateList.valueOf(binding.context.getThemeColor(contentColorAttr))

        loadImage(uri = item.uri.toAndroidUri(), itemId = messageId)
    }

    fun loadImage(uri: Uri, itemId: String) {
        binding.chatImageMessageImageView.load(uri) {
            memoryCacheKey(itemId)
            placeholderMemoryCacheKey(itemId)
            listener(onSuccess = { _, _ -> binding.chatImageMessageLoadingProgressBar.visibility = View.GONE })
        }
    }

    companion object {
        fun create(
            @AttrRes contentColorAttr: Int,
            inflater: LayoutInflater,
            parent: ViewGroup,
        ): ImageMessageContentBinder {
            return ImageMessageContentBinder(
                contentColorAttr = contentColorAttr,
                binding = NablaConversationTimelineItemImageMessageBinding.inflate(inflater, parent, true)
            )
        }
    }
}
