package com.nabla.sdk.core

import android.annotation.SuppressLint
import com.nabla.sdk.core.NablaClient.Companion.getInstance
import com.nabla.sdk.core.NablaClient.Companion.initialize
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.exception.mapFailureAsNablaException
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.ConfigurationException
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.toId
import com.nabla.sdk.core.injection.CoreContainer

/**
 * Main entry-point to SDK-wide features.
 *
 * This also serves for creating instances of [com.nabla.sdk.messaging.core.NablaMessagingClient].
 *
 * We recommend you reuse the same instance for all interactions,
 * check documentation of [initialize] and [getInstance].
 */
public class NablaClient private constructor(
    private val name: String,
    private val configuration: Configuration,
    networkConfiguration: NetworkConfiguration,
) {

    private val coreContainerDelegate = lazy {
        CoreContainer(name, configuration, networkConfiguration)
    }

    @NablaInternal
    public val coreContainer: CoreContainer by coreContainerDelegate

    /**
     * Authenticate the current user with the given [userId].
     *
     * Note that this method isn't suspending as it doesn't directly call the [sessionTokenProvider]
     * but rather stores it to use it later if needed for an authenticated call.
     *
     * @param userId user identifier, typically created on Nabla server and communicated to your own server.
     * @param sessionTokenProvider Callback to get server-made authentication tokens, see [SessionTokenProvider].
     *
     * @see com.nabla.sdk.core.domain.boundary.SessionTokenProvider
     */
    public fun authenticate(
        userId: String,
        sessionTokenProvider: SessionTokenProvider,
    ) {
        runCatching {
            coreContainer.loginInteractor().login(userId.toId(), sessionTokenProvider)
        }.mapFailureAsNablaException(coreContainer.exceptionMapper)
    }

    public companion object {
        private const val DEFAULT_NAMESPACE = "nabla-core"

        @SuppressLint("StaticFieldLeak")
        private var defaultSingletonInstance: NablaClient? = null

        /**
         * Getter for the singleton instance created by [initialize] with a null name.
         * If all your previous calls to [initialize] did specify a name, you're supposed to keep a reference
         * to their returned [NablaClient] instances.
         *
         * @throws NablaException.Configuration.MissingInitialize If you didn't call [initialize] with a null
         * name before.
         *
         * @see initialize
         */
        public fun getInstance(): NablaClient {
            return defaultSingletonInstance ?: throw ConfigurationException.MissingInitialize
        }

        /**
         * Mandatory call to create an instance of [NablaClient].
         *
         * You either:
         *  - Don't specify a [name] and a singleton instance will be created and exposed in [getInstance].
         *  - Or do specify a [name] but then will be responsible of maintaining the returned reference
         *   and passing it to other components of the SDK relying on [NablaClient],
         *   for instance [com.nabla.sdk.messaging.core.NablaMessagingClient.initialize].
         *
         * @param configuration optional configuration if you're not using the manifest for the API key or you want to override some defaults
         * @param networkConfiguration optional network configuration, exposed for internal tests purposes and should not be used in your app
         * @param name optional name to create your own instance to manage
         */
        public fun initialize(
            configuration: Configuration = Configuration(),
            networkConfiguration: NetworkConfiguration = NetworkConfiguration(),
            name: String? = null,
        ): NablaClient {
            synchronized(this) {
                return if (name == null) {
                    val defaultInstance = defaultSingletonInstance
                    if (defaultInstance != null) {
                        defaultInstance
                            .configuration
                            .logger
                            .warn(
                                "NablaClient.initialize() with no specified name should only be called once. " +
                                    "Ignoring this call and using the previously created shared instance. " +
                                    "Use getInstance() to get the previously created instance"
                            )

                        defaultInstance
                    } else {
                        NablaClient(DEFAULT_NAMESPACE, configuration, networkConfiguration)
                            .also { defaultSingletonInstance = it }
                    }
                } else {
                    NablaClient(name, configuration, networkConfiguration)
                }
            }
        }
    }
}
