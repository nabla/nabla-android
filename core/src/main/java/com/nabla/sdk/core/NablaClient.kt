package com.nabla.sdk.core

import com.nabla.sdk.core.NablaClient.Companion.getInstance
import com.nabla.sdk.core.NablaClient.Companion.initialize
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.boundary.Module
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.AuthenticationException
import com.nabla.sdk.core.domain.entity.ConfigurationException
import com.nabla.sdk.core.domain.entity.EventsConnectionState
import com.nabla.sdk.core.domain.entity.toId
import com.nabla.sdk.core.injection.CoreContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Main entry-point to SDK-wide features.
 *
 * This also serves for creating instances of [com.nabla.sdk.messaging.core.NablaMessagingClient].
 *
 * We recommend you reuse the same instance for all interactions,
 * check documentation of [initialize] and [getInstance].
 */
public class NablaClient private constructor(
    modulesFactory: List<Module.Factory<out Module<*>>>,
    private val configuration: Configuration,
    public val name: String,
    sessionTokenProvider: SessionTokenProvider,
) {

    private val coreContainerDelegate = lazy {
        CoreContainer(
            modulesFactory,
            configuration,
            configuration.networkConfiguration,
            name,
            sessionTokenProvider,
        )
    }

    @NablaInternal
    public val coreContainer: CoreContainer by coreContainerDelegate

    init {
        coreContainer.backgroundScope.launch {
            coreContainer.patientRepository.getPatientIdFlow().filterNotNull().collectLatest { patientId ->
                coreContainer.deviceRepository.sendDeviceInfoAsync(coreContainer.activeModules(), patientId)
            }
        }
    }

    /**
     * Set the user to be used by this SDK instance.
     *
     * @param userId user identifier, an arbitrary string that must be unique to this user.
     * Calling this method again with another userId is considered as authenticating a new user
     * by the SDK, so it should be preceded by a call to [clearCurrentUser].
     *
     * @throws AuthenticationException.CurrentUserAlreadySet if a user is already set and is different from the one provided.
     * You should call [clearCurrentUser] before calling this method again with another user id.
     */
    public fun setCurrentUserOrThrow(userId: String) {
        val patientId = userId.toId()
        val existingPatientId = coreContainer.patientRepository.getPatientId()
        if (existingPatientId != null && existingPatientId != patientId) {
            throw AuthenticationException.CurrentUserAlreadySet(existingPatientId.value, patientId.value)
        }
        coreContainer.patientRepository.setPatientId(patientId)
    }

    /**
     * Clear the user currently used by this SDK instance alongside all its data.
     */
    public suspend fun clearCurrentUser() {
        coreContainer.logoutInteractor.logout()
    }

    /**
     * Get the user currently used by this SDK instance.
     */
    public val currentUserId: String? get() = coreContainer.patientRepository.getPatientId()?.value

    /**
     * Watch the state of the events connection the SDK is using to receive live updates (new messages, new appointments etc...)
     *
     * You can use this to display a message to the user indicating they are offline if your use case
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
            return INSTANCES[name] ?: throw ConfigurationException.MissingInitialize()
        }

        /**
         * Mandatory call to create the default instance of [NablaClient].
         * A default instance will be created and exposed in [getInstance].
         *
         * @param modules list of modules to be used by the SDK.
         * @param configuration optional configuration if you're not using the manifest for the API key or you want to override some defaults.
         * @param sessionTokenProvider Callback to get server-made authentication tokens, see [SessionTokenProvider].
         *
         * @see com.nabla.sdk.core.domain.boundary.SessionTokenProvider
         */
        public fun initialize(
            modules: List<Module.Factory<out Module<*>>>,
            configuration: Configuration = Configuration(),
            sessionTokenProvider: SessionTokenProvider,
        ): NablaClient {
            return initialize(modules, configuration, DEFAULT_NAME, sessionTokenProvider)
        }

        /**
         * Mandatory call to create a named instance of [NablaClient].
         * The instance will be created and exposed using the same name in [getInstance].
         *
         * @param modules list of modules to be used by the SDK.
         * @param configuration optional configuration if you're not using the manifest for the API key or you want to override some defaults.
         * @param sessionTokenProvider Callback to get server-made authentication tokens, see [SessionTokenProvider].
         * @param name name to create your own instance, if not specified a default name is used.
         *
         * @see com.nabla.sdk.core.domain.boundary.SessionTokenProvider
         */
        public fun initialize(
            modules: List<Module.Factory<out Module<*>>>,
            configuration: Configuration = Configuration(),
            name: String,
            sessionTokenProvider: SessionTokenProvider,
        ): NablaClient {
            synchronized(this) {
                val alreadyInitializedInstance = INSTANCES[name]
                return if (alreadyInitializedInstance == null) {
                    NablaClient(modules, configuration, name, sessionTokenProvider)
                        .also { INSTANCES[name] = it }
                } else {
                    alreadyInitializedInstance
                        .configuration
                        .logger
                        .warn(
                            "NablaClient.initialize() should only be called once per instance name. " +
                                "Ignoring this call and using the previously created shared instance. " +
                                "Use getInstance(name) to get the previously created instance",
                        )

                    alreadyInitializedInstance
                }
            }
        }
    }
}
