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
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.load
import coil.loadAny
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Size
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.mediapicker.LocalMedia
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemMediaToSendBinding
import java.lang.IllegalStateException
import kotlin.math.max

internal class MediasToSendAdapter(
    private val onMediaClickedListener: (LocalMedia) -> Unit,
    private val onDeleteMediaToSendClickListener: (LocalMedia) -> Unit,
) : ListAdapter<LocalMedia, MediasToSendAdapter.MediaViewHolder>(DIFF_UTIL_CALLBACK) {

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

        fun bindDocument(media: LocalMedia.Document) {
            binding.chatMediaToSendLoadingProgressBar.visibility = View.GONE
            binding.chatMediaToSendImageView.loadAny(media) {
                placeholder(R.drawable.nabla_file_placeholder)
                error(R.drawable.nabla_file_placeholder)
                if (media.mimeType == MimeType.Application.PDF) {
                    fetcher(makeLocalPdfPreviewFetcher(binding.context))
                }
            }
        }

        private fun makeLocalPdfPreviewFetcher(context: Context) = object : Fetcher<LocalMedia.Document> {
            override suspend fun fetch(pool: BitmapPool, data: LocalMedia.Document, size: Size, options: Options): FetchResult {
                val fileDescriptor = context.contentResolver.openFileDescriptor(data.uri.toAndroidUri(), "r")
                if (fileDescriptor != null) {
                    val pdfRenderer = PdfRenderer(fileDescriptor)
                    val firstPage = pdfRenderer.openPage(0)
                    val targetSize = when (size) {
                        is OriginalSize -> PixelSize(firstPage.width, firstPage.height)
                        is PixelSize -> size
                    }
                    val bitmap = pool.get(targetSize.width, targetSize.height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(context.getColor(R.color.white))
                    val xScale = targetSize.width.toFloat() / firstPage.width.toFloat()
                    val yScale = targetSize.height.toFloat() / firstPage.height.toFloat()
                    val cropScale = max(xScale, yScale)
                    val keepAspectRatioMatrix = Matrix().apply { setScale(cropScale, cropScale) }
                    firstPage.render(bitmap, null, keepAspectRatioMatrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    firstPage.close()
                    pdfRenderer.close()
                    return DrawableResult(drawable = bitmap.toDrawable(context.resources), isSampled = false, DataSource.DISK)
                } else {
                    throw NablaException.Internal(IllegalStateException("Can't get a file descriptor from $data"))
                }
            }

            override fun key(data: LocalMedia.Document): String = data.uri.toString()
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
