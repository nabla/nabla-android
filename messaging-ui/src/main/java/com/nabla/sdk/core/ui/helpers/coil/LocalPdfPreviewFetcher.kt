package com.nabla.sdk.core.ui.helpers.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import androidx.core.graphics.drawable.toDrawable
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import com.nabla.sdk.core.R
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.domain.entity.InternalException
import java.net.URI
import kotlin.math.max

internal class LocalPdfPreviewFetcher(
    private val context: Context,
    private val documentUri: URI,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val fileDescriptor = context.contentResolver.openFileDescriptor(documentUri.toAndroidUri(), "r")
        return if (fileDescriptor != null) {
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
            bitmap.eraseColor(context.getColor(R.color.nabla_white))
            val xScale = targetWidth.px.toFloat() / firstPage.width.toFloat()
            val yScale = targetHeight.px.toFloat() / firstPage.height.toFloat()
            val cropScale = max(xScale, yScale)
            val keepAspectRatioMatrix = Matrix().apply { setScale(cropScale, cropScale) }
            firstPage.render(bitmap, null, keepAspectRatioMatrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            firstPage.close()
            pdfRenderer.close()
            DrawableResult(drawable = bitmap.toDrawable(context.resources), isSampled = false, DataSource.DISK)
        } else {
            InternalException.throwNablaInternalException("Can't get a file descriptor from $documentUri")
        }
    }
}
