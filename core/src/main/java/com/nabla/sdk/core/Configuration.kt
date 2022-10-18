package com.nabla.sdk.core

import android.content.Context
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.ConfigurationException
import com.nabla.sdk.core.domain.entity.LogcatLogger

/**
 * Nabla SDK Configuration parameters.
 *
 * @param context Optional as the SDK will fallback to your application context if not specified.
 * @param publicApiKey Optional if already specified in manifest — your organisation's API key (typically created online on Nabla dashboard).
 * @param logger Optional — the logger used by the SDK. You can pass your own logger instance or pass another [LogcatLogger] with a
 * different [LogcatLogger.LogLevel]. Default is [LogcatLogger.LogLevel.WARN].
 */
public class Configuration(
    context: Context = defaultAppContext ?: throw ConfigurationException.MissingContext,
    internal val publicApiKey: String = defaultPublicApiKey ?: throw ConfigurationException.MissingApiKey,
    public val logger: Logger = LogcatLogger(),
    public val enableReporting: Boolean = true,
) {
    @NablaInternal
    public val context: Context = context.applicationContext

    @NablaInternal
    public companion object {
        internal var defaultAppContext: Context? = null
        internal var defaultPublicApiKey: String? = null
    }
}

public data class Header(val name: String, val value: String)

public interface HeaderProvider {
    public fun headers(): List<Header>
}
