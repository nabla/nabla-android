package com.nabla.sdk.core.ui.model

import androidx.annotation.StringRes
import com.nabla.sdk.core.R
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.exception.isNetworkError
import com.nabla.sdk.core.databinding.NablaErrorLayoutBinding
import com.nabla.sdk.core.ui.helpers.setTextOrHide

@NablaInternal
public data class ErrorUiModel(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
) {
    public companion object {
        public val Network: ErrorUiModel = ErrorUiModel(
            titleRes = R.string.nabla_error_message_network_title,
            bodyRes = R.string.nabla_error_message_network_body,
        )

        public val Generic: ErrorUiModel = ErrorUiModel(
            titleRes = R.string.nabla_error_message_generic_title,
            bodyRes = R.string.nabla_error_message_generic_body,
        )
    }
}

@NablaInternal
public fun NablaErrorLayoutBinding.bind(error: ErrorUiModel, onRetryListener: () -> Unit) {
    nablaErrorTitleTextView.setTextOrHide(error.titleRes)
    nablaErrorBodyTextView.setTextOrHide(error.bodyRes)
    nablaErrorRetryButton.setOnClickListener { onRetryListener() }
}

@NablaInternal
public val Throwable.asNetworkOrGeneric: ErrorUiModel
    get() = if (this.isNetworkError()) ErrorUiModel.Network else ErrorUiModel.Generic
