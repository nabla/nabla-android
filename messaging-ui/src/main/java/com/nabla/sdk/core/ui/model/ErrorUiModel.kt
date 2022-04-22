package com.nabla.sdk.core.ui.model

import androidx.annotation.StringRes
import com.nabla.sdk.core.ui.helpers.setTextOrHide
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaErrorLayoutBinding

internal data class ErrorUiModel(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
) {
    companion object {
        val Network = ErrorUiModel(
            titleRes = R.string.nabla_error_message_network_title,
            bodyRes = R.string.nabla_error_message_network_body,
        )

        val Generic = ErrorUiModel(
            titleRes = R.string.nabla_error_message_generic_title,
            bodyRes = R.string.nabla_error_message_generic_body,
        )
    }
}

internal fun NablaErrorLayoutBinding.bind(error: ErrorUiModel, onRetryListener: () -> Unit) {
    nablaErrorTitleTextView.setTextOrHide(error.titleRes)
    nablaErrorBodyTextView.setTextOrHide(error.bodyRes)
    nablaErrorRetryButton.setOnClickListener { onRetryListener() }
}
