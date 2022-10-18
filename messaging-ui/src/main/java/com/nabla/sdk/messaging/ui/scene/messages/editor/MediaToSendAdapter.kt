package com.nabla.sdk.messaging.ui.scene.messages.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.decode.DataSource
import coil.decode.VideoFrameDecoder
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.load
import coil.request.Options
import coil.size.Dimension
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.domain.entity.InternalException.Companion.asNablaInternal
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.mediapicker.LocalMedia
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemMediaToSendBinding
import kotlin.math.max
import com.nabla.sdk.core.R as coreR

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
            )
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
                    }
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
                        makeLocalPdfPreviewFetcher(binding.context, data, options)
                    }
                }
            }
        }

        private fun makeLocalPdfPreviewFetcher(
            context: Context,
            data: LocalMedia.Document,
            options: Options,
        ) = Fetcher {
            val fileDescriptor = context.contentResolver.openFileDescriptor(data.uri.toAndroidUri(), "r")
            if (fileDescriptor != null) {
                val pdfRenderer = PdfRenderer(fileDescriptor)
                val firstPage = pdfRenderer.openPage(0)
                val (width, height) = options.size
                val (targetWidth, targetHeight) = if (width is Dimension.Pixels && height is Dimension.Pixels) {
                    width to height
                } else {
                    Dimension.Pixels(firstPage.width) to Dimension.Pixels(firstPage.height)
                }
                val bitmap = Bitmap.createBitmap(
                    targetWidth.px,
                    targetHeight.px,
                    Bitmap.Config.ARGB_8888,
                )
                bitmap.eraseColor(context.getColor(coreR.color.nabla_white))
                val xScale = targetWidth.px.toFloat() / firstPage.width.toFloat()
                val yScale = targetHeight.px.toFloat() / firstPage.height.toFloat()
                val cropScale = max(xScale, yScale)
                val keepAspectRatioMatrix = Matrix().apply { setScale(cropScale, cropScale) }
                firstPage.render(bitmap, null, keepAspectRatioMatrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                firstPage.close()
                pdfRenderer.close()
                DrawableResult(drawable = bitmap.toDrawable(context.resources), isSampled = false, DataSource.DISK)
            } else {
                throw IllegalStateException("Can't get a file descriptor from $data").asNablaInternal()
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
