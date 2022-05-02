package com.nabla.sdk.core

import android.annotation.SuppressLint
import com.nabla.sdk.core.NablaCore.Companion.getInstance
import com.nabla.sdk.core.NablaCore.Companion.initialize
import com.nabla.sdk.core.data.exception.mapFailureAsNablaException
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
class NablaCore private constructor(
    private val name: String,
    private val nablaCoreConfig: NablaCoreConfig,
) {

    private val coreContainerDelegate = lazy {
        CoreContainer(name, nablaCoreConfig)
    }
    internal val coreContainer by coreContainerDelegate

    /**
     * Authenticate the current user.
     *
     * This relies on the [NablaCoreConfig.sessionTokenProvider] callback specified during SDK configuration.
     *
     * @param userId user identifier, typically created on Nabla server and communicated to your own server.
     *
     * @see initialize
     * @see com.nabla.sdk.core.domain.boundary.SessionTokenProvider
     */
    suspend fun authenticate(userId: String): Result<Unit> {
        return coreContainer.loginInteractor().invoke(userId.toId())
            .mapFailureAsNablaException(coreContainer.exceptionMapper)
    }

    companion object {
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
        fun getInstance(): NablaCore {
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
         */
        fun initialize(nablaCoreConfig: NablaCoreConfig, name: String? = null): NablaCore {
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
