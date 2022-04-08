package com.nabla.sdk.messaging.ui.helper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService

internal fun Context.copyNewPlainText(label: String, text: String) {
    val data = ClipData.newPlainText(label, text)
    getSystemService<ClipboardManager>()?.setPrimaryClip(data)
}
