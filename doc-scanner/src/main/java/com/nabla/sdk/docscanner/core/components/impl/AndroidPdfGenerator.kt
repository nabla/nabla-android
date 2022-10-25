package com.nabla.sdk.docscanner.core.components.impl

import android.content.Context
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.data.helper.toKtUri
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.helpers.toJavaDate
import com.nabla.sdk.docscanner.R
import com.nabla.sdk.docscanner.core.components.PdfGenerator
import com.nabla.sdk.docscanner.core.helpers.getWarpMatrix
import com.nabla.sdk.docscanner.core.models.NormalizedCorners
import com.nabla.sdk.docscanner.core.providers.DocumentScanProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

internal class AndroidPdfGenerator(
    private val applicationContext: Context,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock,
) : PdfGenerator {
    override suspend fun generatePdf(imageUriWithCorners: List<Pair<Uri, NormalizedCorners?>>): Uri {
        val name = generateName()

        @Suppress("BlockingMethodInNonBlockingContext") // createNewFile & FileOutputStream
        return withContext(backgroundDispatcher) {
            val pdfDocument = PdfDocument()
            try {
                imageUriWithCorners.forEachIndexed { index, (imageUri, corners) ->
                    val bitmap = applicationContext.getBitmapFromUri(imageUri.toAndroidUri(), backgroundDispatcher = backgroundDispatcher)
                    if (corners == null) {
                        val matrix = Matrix().apply { setScale(1f, 1f) }
                        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, matrix, null)
                        pdfDocument.finishPage(page)
                    } else {
                        val width = bitmap.width * corners.getWidthRatio()
                        val height = bitmap.height * corners.getHeightRatio()
                        val matrix = corners.getWarpMatrix(bitmap)
                        val pageInfo = PdfDocument.PageInfo.Builder(width.toInt(), height.toInt(), index + 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, matrix, null)
                        pdfDocument.finishPage(page)
                    }
                }
                val destinationFile = DocumentScanProvider.createFile(applicationContext, name)
                destinationFile.delete()
                destinationFile.createNewFile()
                val fileOutputStream = FileOutputStream(destinationFile, false)
                fileOutputStream.use {
                    pdfDocument.writeTo(fileOutputStream)
                }
                DocumentScanProvider.getUri(applicationContext, destinationFile).toKtUri()
            } finally {
                pdfDocument.close()
            }
        }
    }

    private fun generateName(): String {
        val nameDateFormat = SimpleDateFormat(
            applicationContext.getString(R.string.nabla_document_scan_new_document_name_date_format),
            Locale.getDefault(),
        )
        return applicationContext.getString(
            R.string.nabla_document_scan_new_document_name,
            nameDateFormat.format(clock.now().toJavaDate())
        ) + ".pdf"
    }
}
