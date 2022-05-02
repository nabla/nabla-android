package com.nabla.sdk.core

import android.content.Context
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.messaging.core.BuildConfig

class NablaCoreConfig(
    context: Context = defaultAppContext ?: throw NablaException.Configuration.MissingContext,
    val publicApiKey: String = defaultPublicApiKey ?: throw NablaException.Configuration.MissingApiKey,
    val baseUrl: String = "https://api.nabla.com/",
    val isLoggingEnabled: Boolean = BuildConfig.DEBUG,
    val additionalHeadersProvider: HeaderProvider? = null,
    val sessionTokenProvider: SessionTokenProvider,
) {
    val context: Context = context.applicationContext

    companion object {
        internal var defaultAppContext: Context? = null
        internal var defaultPublicApiKey: String? = null
    }
}

data class Header(val name: String, val value: String)

interface HeaderProvider {
    fun headers(): List<Header>
}
