package com.nabla.sdk.messaging.ui.scene.messages.editor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.decode.VideoFrameDecoder
import coil.load
import com.nabla.sdk.core.data.helper.UrlExt.toAndroidUri
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.core.ui.helpers.coil.LocalPdfPreviewFetcher
import com.nabla.sdk.core.ui.helpers.mediapicker.LocalMedia
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemMediaToSendBinding

internal class MediaToSendAdapter(
    private val onMediaClickedListener: (LocalMedia) -> Unit,
    private val onDeleteMediaToSendClickListener: (LocalMedia) -> Unit,
    private val onErrorLoadingVideoThumbnail: (Throwable) -> Unit,
) : ListAdapter<LocalMedia, MediaToSendAdapter.MediaViewHolder>(DIFF_UTIL_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        return MediaViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.nabla_conversation_timeline_item_media_to_send,
                parent,
                false,
            ),
        )
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val media = getItem(position)
        when (media) {
            is LocalMedia.Image -> holder.bindImage(media)
            is LocalMedia.Video -> holder.bindVideo(media, onErrorLoadingVideoThumbnail)
            is LocalMedia.Document -> holder.bindDocument(media)
        }

        holder.binding.chatMediaToSendRemoveButton.setOnClickListener {
            onDeleteMediaToSendClickListener(media)
        }

        holder.binding.chatMediaToSendImageView.setOnClickListener {
            onMediaClickedListener(media)
        }
    }

    class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = NablaConversationTimelineItemMediaToSendBinding.bind(view)

        fun bindImage(media: LocalMedia.Image) {
            binding.chatMediaToSendImageView.visibility = View.INVISIBLE
            binding.chatMediaToSendLoadingProgressBar.visibility = View.VISIBLE

            binding.chatMediaToSendImageView.load(media.uri.toAndroidUri()) {
                listener(
                    onSuccess = { _, _ ->
                        binding.chatMediaToSendImageView.visibility = View.VISIBLE
                        binding.chatMediaToSendLoadingProgressBar.visibility = View.GONE
                    },
                )
            }
        }

        fun bindVideo(media: LocalMedia.Video, onErrorLoadingThumbnail: (Throwable) -> Unit) {
            binding.chatMediaToSendImageView.visibility = View.INVISIBLE
            binding.chatMediaToSendLoadingProgressBar.visibility = View.VISIBLE

            binding.chatMediaToSendImageView.load(media.uri.toAndroidUri()) {
                decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }

                listener(
                    onSuccess = { _, _ ->
                        binding.chatMediaToSendImageView.visibility = View.VISIBLE
                        binding.chatMediaToSendLoadingProgressBar.visibility = View.GONE
                    },
                    onError = { _, error ->
                        onErrorLoadingThumbnail(error.throwable)
                    },
                )
            }
        }

        fun bindDocument(media: LocalMedia.Document) {
            binding.chatMediaToSendLoadingProgressBar.visibility = View.GONE
            binding.chatMediaToSendImageView.load(media) {
                placeholder(R.drawable.nabla_file_placeholder)
                error(R.drawable.nabla_file_placeholder)
                if (media.mimeType == MimeType.Application.Pdf) {
                    fetcherFactory<LocalMedia.Document> { data, options, _ ->
                        LocalPdfPreviewFetcher(binding.context, data.uri, options)
                    }
                }
            }
        }
    }

    companion object {
        private val DIFF_UTIL_CALLBACK = object : DiffUtil.ItemCallback<LocalMedia>() {
            override fun areItemsTheSame(oldItem: LocalMedia, newItem: LocalMedia): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: LocalMedia, newItem: LocalMedia): Boolean {
                return oldItem == newItem
            }
        }
    }
}
