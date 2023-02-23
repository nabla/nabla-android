package com.nabla.sdk.core.ui.model

import com.nabla.sdk.core.R
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.exception.isNetworkError
import com.nabla.sdk.core.databinding.NablaErrorLayoutBinding
import com.nabla.sdk.core.domain.entity.StringOrRes
import com.nabla.sdk.core.domain.entity.StringOrRes.Companion.asStringOrRes
import com.nabla.sdk.core.domain.entity.StringOrRes.Companion.evaluate
import com.nabla.sdk.core.ui.helpers.TextViewExtension.setTextOrHide

@NablaInternal
public data class ErrorUiModel(
    val title: StringOrRes,
    val body: StringOrRes,
) {
    @NablaInternal
    public companion object {
        public val Network: ErrorUiModel = ErrorUiModel(
            title = R.string.nabla_error_message_network_title.asStringOrRes(),
            body = R.string.nabla_error_message_network_body.asStringOrRes(),
        )

        public val Generic: ErrorUiModel = ErrorUiModel(
            title = R.string.nabla_error_message_generic_title.asStringOrRes(),
            body = R.string.nabla_error_message_generic_body.asStringOrRes(),
        )

        public fun NablaErrorLayoutBinding.bind(error: ErrorUiModel, onRetryListener: () -> Unit) {
            nablaErrorTitleTextView.setTextOrHide(error.title.evaluate(this))
            nablaErrorBodyTextView.setTextOrHide(error.body.evaluate(this))
            nablaErrorRetryButton.setOnClickListener { onRetryListener() }
        }

        public val Throwable.asNetworkOrGeneric: ErrorUiModel
            get() = if (this.isNetworkError()) Network else Generic
    }
}
