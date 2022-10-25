package com.nabla.sdk.docscanner.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException

@NablaInternal
public class DocScannerActivityContract :
    ActivityResultContract<String, DocScannerActivityContract.Result>() {

    override fun createIntent(context: Context, input: String): Intent {
        return DocumentScanActivity.newIntent(context, name = input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        return try {
            if (resultCode == Activity.RESULT_CANCELED) {
                return Result.Cancelled
            }

            if (resultCode != Activity.RESULT_OK || intent == null) {
                throwNablaInternalException("Activity result: $resultCode")
            }

            intent.getParcelableExtra<Uri>(DocumentScanActivity.EXTRA_RESULT_IMAGE_URI)
                ?.let { Result.Document(it) }
                ?: throwNablaInternalException("No parcelable extra in the intent with key ${DocumentScanActivity.EXTRA_RESULT_IMAGE_URI}")
        } catch (error: Exception) {
            Result.Failed(error)
        }
    }

    public sealed interface Result {
        public object Cancelled : Result
        public data class Failed(val error: Throwable) : Result
        public data class Document(val uri: Uri) : Result
    }
}
