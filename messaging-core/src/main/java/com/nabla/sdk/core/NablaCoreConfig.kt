package com.nabla.sdk.core

import android.content.Context
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.messaging.core.BuildConfig

/**
 * Nabla SDK Configuration parameters.
 *
 * @param context Optional as the SDK will fallback to your application context if not specified.
 * @param publicApiKey Optional if already specified in manifest — your organisation's API key (typically created online on Nabla dashboard).
 * @param baseUrl Optional — base url for Nabla API server. This is exposed for internal usage and you should probably not use it in your app.
 * @param isLoggingEnabled Optional — whether to verbosely log or not.
 * @param additionalHeadersProvider Optional — useful to append additional query headers to http calls.
 *        This is exposed for internal usage and you should probably not use it in your app.
 * @param sessionTokenProvider Callback to get server-made authentication tokens, see [SessionTokenProvider].
 */
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
