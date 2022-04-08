package com.nabla.sdk.core.ui.helpers.mediapicker

import com.nabla.sdk.core.domain.entity.MimeType
import org.overviewproject.mime_types.MimeTypeDetector
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

internal object MimeTypeHelper {
    private val detector = MimeTypeDetector()

    @Throws(Exception::class)
    fun getFileMediaMimeType(file: File): MimeType = MimeType.fromStringRepresentation(
        detector.detectMimeType(file.path, BufferedInputStream(FileInputStream(file)))
    )
}
