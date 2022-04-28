package com.nabla.sdk.core

import android.content.Context
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.messaging.core.BuildConfig

class NablaCoreConfig private constructor(
    val context: Context,
    val publicApiKey: String,
    val baseUrl: String,
    val isLoggingEnabled: Boolean,
    val sessionTokenProvider: SessionTokenProvider,
    val additionalHeadersProvider: HeaderProvider?
) {

    class Builder(val sessionTokenProvider: SessionTokenProvider) {
        private var context: Context? = null
        private var publicApiKey: String? = null
        private var baseUrl: String = "https://api.nabla.com/"
        private var isLoggingEnabled: Boolean = BuildConfig.DEBUG
        private var additionalHeadersProvider: HeaderProvider? = null

        fun context(context: Context) = apply { this.context = context.applicationContext }
        fun publicApiKey(publicApiKey: String) = apply { this.publicApiKey = publicApiKey }
        fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
        fun isLoggingEnabled(isLoggingEnabled: Boolean) = apply {
            this.isLoggingEnabled = isLoggingEnabled
        }
        fun additionalHeadersProvider(additionalHeadersProvider: HeaderProvider) = apply {
            this.additionalHeadersProvider = additionalHeadersProvider
        }

        fun build(): NablaCoreConfig {
            return NablaCoreConfig(
                context = context ?: defaultAppContext ?: throw NablaException.Configuration.MissingContext,
                publicApiKey = publicApiKey ?: defaultPublicApiKey ?: throw NablaException.Configuration.MissingApiKey,
                baseUrl = baseUrl,
                isLoggingEnabled = isLoggingEnabled,
                sessionTokenProvider = sessionTokenProvider,
                additionalHeadersProvider = additionalHeadersProvider
            )
        }
    }

    companion object {
        internal var defaultAppContext: Context? = null
        internal var defaultPublicApiKey: String? = null
    }
}

data class Header(val name: String, val value: String)

interface HeaderProvider {
    fun headers(): List<Header>
}
