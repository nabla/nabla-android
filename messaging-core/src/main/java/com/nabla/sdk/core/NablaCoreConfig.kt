package com.nabla.sdk.core

import android.content.Context
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
 */
public class NablaCoreConfig(
    context: Context = defaultAppContext ?: throw NablaException.Configuration.MissingContext,
    internal val publicApiKey: String = defaultPublicApiKey ?: throw NablaException.Configuration.MissingApiKey,
    internal val baseUrl: String = "https://api.nabla.com/",
    internal val isLoggingEnabled: Boolean = BuildConfig.DEBUG,
    internal val additionalHeadersProvider: HeaderProvider? = null,
) {
    internal val context: Context = context.applicationContext

    internal companion object {
        internal var defaultAppContext: Context? = null
        internal var defaultPublicApiKey: String? = null
    }
}

public data class Header(val name: String, val value: String)

public interface HeaderProvider {
    public fun headers(): List<Header>
}
