package com.nabla.sdk.docscanner.ui

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Size
import coil.transform.Transformation
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.docscanner.R
import com.nabla.sdk.docscanner.core.helpers.getWarpedBitmap
import com.nabla.sdk.docscanner.core.models.NormalizedCorners
import com.nabla.sdk.docscanner.databinding.NablaItemProcessedDocumentBinding

internal class DocumentAdapter : ListAdapter<DocumentScanBuilderViewModel.ProcessedImage, DocumentAdapter.DocumentViewHolder>(DIFF_UTIL_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        return DocumentViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.nabla_item_processed_document, parent, false)
        )
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DocumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = NablaItemProcessedDocumentBinding.bind(view)

        fun bind(item: DocumentScanBuilderViewModel.ProcessedImage) {
            binding.image.load(item.image.uri.toAndroidUri()) {
                item.documentCorners?.let {
                    transformations(makeWarpTransform(it))
                }
            }
        }

        private fun makeWarpTransform(corners: NormalizedCorners) = object : Transformation {
            override val cacheKey: String = corners.toString()

            override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                return getWarpedBitmap(input, corners) { width, height ->
                    Bitmap.createBitmap(width, height, input.config)
                }
            }
        }
    }

    companion object {
        private val DIFF_UTIL_CALLBACK = object : DiffUtil.ItemCallback<DocumentScanBuilderViewModel.ProcessedImage>() {
            override fun areItemsTheSame(
                oldItem: DocumentScanBuilderViewModel.ProcessedImage,
                newItem: DocumentScanBuilderViewModel.ProcessedImage,
            ): Boolean = oldItem.image == newItem.image

            override fun areContentsTheSame(
                oldItem: DocumentScanBuilderViewModel.ProcessedImage,
                newItem: DocumentScanBuilderViewModel.ProcessedImage,
            ): Boolean = oldItem == newItem
        }
    }
}
