package com.nabla.sdk.core

import com.nabla.sdk.core.NablaClient.Companion.getInstance
import com.nabla.sdk.core.NablaClient.Companion.initialize
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.exception.mapFailureAsNablaException
import com.nabla.sdk.core.domain.boundary.Module
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.ConfigurationException
import com.nabla.sdk.core.domain.entity.EventsConnectionState
import com.nabla.sdk.core.domain.entity.toId
import com.nabla.sdk.core.injection.CoreContainer
import kotlinx.coroutines.flow.Flow

/**
 * Main entry-point to SDK-wide features.
 *
 * This also serves for creating instances of [com.nabla.sdk.messaging.core.NablaMessagingClient].
 *
 * We recommend you reuse the same instance for all interactions,
 * check documentation of [initialize] and [getInstance].
 */
public class NablaClient private constructor(
    public val name: String,
    private val configuration: Configuration,
    networkConfiguration: NetworkConfiguration,
    modulesFactory: List<Module.Factory<out Module>>,
) {

    private val coreContainerDelegate = lazy {
        CoreContainer(
            name,
            configuration,
            networkConfiguration,
            modulesFactory,
        )
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
        }.mapFailureAsNablaException(coreContainer.exceptionMapper).getOrThrow()
    }

    /**
     * Watch the state of the events connection the SDK is using to receive live updates (new messages, new appointments etc...)
     *
     * You can use this to display a message to the user indicating they are offline is your use case
     * is time sensitive and the risk of missing a message is important.
     *
     * Note that when the SDK is initialized, it starts with [EventsConnectionState.NotConnected].
     * The state won't change until you start using the SDK by either displaying some UI or calling a watcher.
     */
    public fun watchEventsConnectionState(): Flow<EventsConnectionState> = coreContainer.eventsConnectionState

    public companion object {
        /**
         * The name of the default instance.
         */
        public const val DEFAULT_NAME: String = "default-nabla-sdk"

        private val INSTANCES = mutableMapOf<String, NablaClient>()

        /**
         * Getter for the default instance created by [initialize].
         *
         * @throws ConfigurationException.MissingInitialize If you didn't call [initialize] before.
         *
         * @see initialize
         */
        public fun getInstance(): NablaClient {
            return getInstance(DEFAULT_NAME)
        }

        /**
         * Getter for a named instance created by [initialize].
         *
         * @param name name of the instance to get, as provided to [initialize].
         *
         * @throws ConfigurationException.MissingInitialize If you didn't call [initialize] with the corresponding name before.
         *
         * @see initialize
         */
        public fun getInstance(name: String): NablaClient {
            return INSTANCES[name] ?: throw ConfigurationException.MissingInitialize
        }

        /**
         * Mandatory call to create the default instance of [NablaClient].
         * A default instance will be created and exposed in [getInstance].
         *
         * @param modules list of modules to be used by the SDK.
         * @param configuration optional configuration if you're not using the manifest for the API key or you want to override some defaults.
         * @param networkConfiguration optional network configuration, exposed for internal tests purposes and should not be used in your app.
         */
        public fun initialize(
            modules: List<Module.Factory<out Module>>,
            configuration: Configuration = Configuration(),
            networkConfiguration: NetworkConfiguration = NetworkConfiguration(),
        ): NablaClient {
            return initialize(modules, configuration, networkConfiguration, DEFAULT_NAME)
        }

        /**
         * Mandatory call to create a named instance of [NablaClient].
         * The instance will be created and exposed using the same name in [getInstance].
         *
         * @param modules list of modules to be used by the SDK.
         * @param configuration optional configuration if you're not using the manifest for the API key or you want to override some defaults.
         * @param networkConfiguration optional network configuration, exposed for internal tests purposes and should not be used in your app.
         * @param name name to create your own instance, if not specified a default name is used.
         */
        public fun initialize(
            modules: List<Module.Factory<out Module>>,
            configuration: Configuration = Configuration(),
            networkConfiguration: NetworkConfiguration = NetworkConfiguration(),
            name: String,
        ): NablaClient {
            synchronized(this) {
                val alreadyInitializedInstance = INSTANCES[name]
                return if (alreadyInitializedInstance == null) {
                    NablaClient(name, configuration, networkConfiguration, modules)
                        .also { INSTANCES[name] = it }
                } else {
                    alreadyInitializedInstance
                        .configuration
                        .logger
                        .warn(
                            "NablaClient.initialize() should only be called once per instance name. " +
                                "Ignoring this call and using the previously created shared instance. " +
                                "Use getInstance(name) to get the previously created instance"
                        )

                    alreadyInitializedInstance
                }
            }
        }
    }
}
