package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import coil.decode.VideoFrameDecoder
import coil.load
import com.google.android.material.resources.TextAppearance
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.getThemeStyle
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemVideoMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

@SuppressLint("RestrictedApi") // TextAppearance class
internal class VideoMessageContentBinder(
    @AttrRes private val contentTextAppearanceAttr: Int,
    private val binding: NablaConversationTimelineItemVideoMessageBinding,
    private val onErrorFetchingVideoThumbnail: (error: Throwable) -> Unit,
) : MessageContentBinder<TimelineItem.Message.Video>() {
    private val contentAppearance = TextAppearance(binding.context, binding.context.getThemeStyle(contentTextAppearanceAttr))

    override fun bind(messageId: String, item: TimelineItem.Message.Video) {
        binding.conversationVideoMessageImageView.visibility = View.INVISIBLE
        binding.conversationVideoMessagePlayIconImageView.visibility = View.GONE
        binding.conversationVideoMessageLoadingProgressBar.visibility = View.VISIBLE
        binding.conversationVideoMessageLoadingProgressBar.indeterminateTintList = contentAppearance.textColor

        loadVideo(item.uri.toAndroidUri(), messageId)
    }

    fun loadVideo(uri: Uri, itemId: String) {
        binding.conversationVideoMessageImageView.load(uri) {
            memoryCacheKey(itemId)
            placeholderMemoryCacheKey(itemId)
            decoder(VideoFrameDecoder(binding.context))

            listener(
                onSuccess = { _, _ ->
                    binding.conversationVideoMessageImageView.visibility = View.VISIBLE
                    binding.conversationVideoMessagePlayIconImageView.visibility = View.VISIBLE
                    binding.conversationVideoMessageLoadingProgressBar.visibility = View.GONE
                },
                onError = { _, error ->
                    onErrorFetchingVideoThumbnail(error)
                }
            )
        }
    }

    companion object {
        fun create(
            @AttrRes contentTextAppearanceAttr: Int,
            inflater: LayoutInflater,
            parent: ViewGroup,
            onErrorFetchingVideoThumbnail: (error: Throwable) -> Unit,
        ): VideoMessageContentBinder {
            return VideoMessageContentBinder(
                contentTextAppearanceAttr = contentTextAppearanceAttr,
                binding = NablaConversationTimelineItemVideoMessageBinding.inflate(inflater, parent, true),
                onErrorFetchingVideoThumbnail = onErrorFetchingVideoThumbnail
            )
        }
    }
}
