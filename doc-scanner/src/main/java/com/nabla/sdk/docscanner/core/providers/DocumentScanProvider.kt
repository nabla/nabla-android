package com.nabla.sdk.docscanner.core.providers

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

internal class DocumentScanProvider : FileProvider() {
    companion object {
        private fun authority(context: Context) = "${context.packageName}.nabla.docscanner.documentscanprovider"

        fun getUri(context: Context, file: File): Uri {
            return getUriForFile(
                context,
                authority(context),
                file,
            )
        }

        fun createFile(context: Context, title: String): File {
            val file = File(context.cacheDir, "nabla/scan/documents/$title")
            file.parentFile?.mkdirs()
            return file
        }
    }
}
