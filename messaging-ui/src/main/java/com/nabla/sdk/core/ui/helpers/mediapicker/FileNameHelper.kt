package com.nabla.sdk.core.ui.helpers.mediapicker

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun generateFileName(extension: String?): String {
    val dateFormatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
    return "${dateFormatter.format(Date())}${if (extension != null) ".$extension" else ""}"
}
