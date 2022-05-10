package com.nabla.sdk.core

import android.annotation.SuppressLint
import com.nabla.sdk.core.NablaCore.Companion.getInstance
import com.nabla.sdk.core.NablaCore.Companion.initialize
import com.nabla.sdk.core.data.exception.mapFailureAsNablaException
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.toId
import com.nabla.sdk.core.injection.CoreContainer

/**
 * Main entry-point to SDK-wide features.
 *
 * This also serves for creating instances of [com.nabla.sdk.messaging.core.NablaMessaging].
 *
 * We recommend you reuse the same instance for all interactions,
 * check documentation of [initialize] and [getInstance].
 */
public class NablaCore private constructor(
    private val name: String,
    private val nablaCoreConfig: NablaCoreConfig,
) {

    private val coreContainerDelegate = lazy {
        CoreContainer(name, nablaCoreConfig)
    }
    internal val coreContainer by coreContainerDelegate

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
        private var defaultSingletonInstance: NablaCore? = null

        /**
         * Getter for the singleton instance created by [initialize] with a null name.
         * If all your previous calls to [initialize] did specify a name, you're supposed to keep a reference
         * to their returned [NablaCore] instances.
         *
         * @throws NablaException.Configuration.MissingInitialize If you didn't call [initialize] with a null
         * name before.
         *
         * @see initialize
         */
        public fun getInstance(): NablaCore {
            return defaultSingletonInstance ?: throw NablaException.Configuration.MissingInitialize
        }

        /**
         * Mandatory call to create an instance of [NablaCore].
         *
         * You either:
         *  - Don't specify a [name] and a singleton instance will be created and exposed in [getInstance].
         *  - Or do specify a [name] but then will be responsible of maintaining the returned reference
         *   and passing it to other components of the SDK relying on [NablaCore],
         *   for instance [com.nabla.sdk.messaging.core.NablaMessaging.initialize].
         *
         * @param nablaCoreConfig optional configuration if you're not using the manifest for the API key or you want to override some defaults
         * @param name optional name to create your own instance to manage
         */
        public fun initialize(nablaCoreConfig: NablaCoreConfig = NablaCoreConfig(), name: String? = null): NablaCore {
            synchronized(this) {
                return if (name == null) {
                    defaultSingletonInstance
                        ?: NablaCore(DEFAULT_NAMESPACE, nablaCoreConfig).also { defaultSingletonInstance = it }
                } else {
                    NablaCore(name, nablaCoreConfig)
                }
            }
        }
    }
}
