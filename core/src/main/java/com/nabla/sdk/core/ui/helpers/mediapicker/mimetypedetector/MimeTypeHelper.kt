package com.nabla.sdk.core.ui.helpers.mediapicker.mimetypedetector

import android.content.Context
import com.nabla.sdk.core.domain.entity.MimeType
import java.io.File

internal class MimeTypeHelper private constructor(context: Context) {
    companion object {
        private var instance: MimeTypeHelper? = null

        fun getInstance(context: Context): MimeTypeHelper {
            return instance ?: kotlin.run {
                val newInstance = MimeTypeHelper(context)
                instance = newInstance
                newInstance
            }
        }
    }

    private val detector = MimeTypeDetector(context.applicationContext)

    @Throws(Exception::class)
    fun getFileMediaMimeType(file: File): MimeType = MimeType.fromStringRepresentation(
        detector.detectMimeType(file),
    )
}
