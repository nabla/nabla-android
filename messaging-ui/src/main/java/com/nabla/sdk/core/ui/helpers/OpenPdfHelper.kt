package com.nabla.sdk.core.ui.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.messaging.ui.R
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URI
import kotlin.coroutines.resume

suspend fun openPdfReader(context: Context, pdfUri: URI): OpenPdfReaderResult {
    val pdfIntent = makePdfIntent(pdfUri)
    return try {
        context.startActivity(pdfIntent)
        OpenPdfReaderResult.Success
    } catch (e: ActivityNotFoundException) {
        askUserToInstallPdfReader(context)
    }
}

private suspend fun askUserToInstallPdfReader(context: Context): OpenPdfReaderResult {
    return suspendCancellableCoroutine { continuation ->
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.no_pdf_third_party_app_info_title)
            .setMessage(R.string.no_pdf_third_party_app_info_message)
            .setPositiveButton(R.string.no_pdf_third_party_app_info_positive) { _, _ ->
                try {
                    context.startActivity(makeWebPlayStoreGooglePdfReaderIntent())
                    continuation.resume(OpenPdfReaderResult.NoPdfReader.PlayStoreOpenedToInstallReaderApp)
                } catch (activityNotFoundException: ActivityNotFoundException) {
                    continuation.resume(OpenPdfReaderResult.NoPdfReader.ErrorOpeningPlayStoreToInstallReaderApp(activityNotFoundException))
                }
            }.setNegativeButton(R.string.no_pdf_third_party_app_info_negative) { _, _ ->
                continuation.resume(OpenPdfReaderResult.NoPdfReader.UserRefusedToInstallReaderApp)
            }.setOnCancelListener {
                continuation.resume(OpenPdfReaderResult.NoPdfReader.UserRefusedToInstallReaderApp)
            }.show()
        continuation.invokeOnCancellation { dialog.dismiss() }
    }
}

private fun makePdfIntent(pdfUri: URI): Intent {
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(pdfUri.toAndroidUri(), "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun makeWebPlayStoreGooglePdfReaderIntent(): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.docs"))
}

sealed class OpenPdfReaderResult {
    object Success : OpenPdfReaderResult()
    sealed class NoPdfReader : OpenPdfReaderResult() {
        object UserRefusedToInstallReaderApp : NoPdfReader()
        class ErrorOpeningPlayStoreToInstallReaderApp(val error: Throwable) : NoPdfReader()
        object PlayStoreOpenedToInstallReaderApp : NoPdfReader()
    }
}
