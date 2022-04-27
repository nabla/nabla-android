package com.nabla.sdk.core

import android.content.Context
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.toId
import com.nabla.sdk.core.injection.CoreContainer

class NablaCore constructor(
    private val name: String,
    private val context: Context,
    private val nablaCoreConfig: NablaCoreConfig,
) {

    private val coreContainerDelegate = lazy {
        CoreContainer(name, context.applicationContext, nablaCoreConfig)
    }
    internal val coreContainer by coreContainerDelegate

    suspend fun authenticate(userId: String): Result<Unit> {
        return coreContainer.loginInteractor().invoke(userId.toId())
    }

    companion object {
        private const val DEFAULT_NAMESPACE = "nabla-core"

        private var defaultAppContext: Context? = null
        private var defaultPublicApiKey: String? = null
        private var defaultSessionTokenProvider: SessionTokenProvider? = null

        private val instances = mutableMapOf<String, NablaCore>()

        internal fun registerDefaultContext(context: Context) {
            defaultAppContext = context.applicationContext
        }

        internal fun registerDefaultPublicApiKey(publicApiKey: String) {
            defaultPublicApiKey = publicApiKey
        }

        fun registerDefaultSessionTokenProvider(sessionTokenProvider: SessionTokenProvider) {
            defaultSessionTokenProvider = sessionTokenProvider
        }

        fun getInstance(): NablaCore {
            return lazyInitialize(DEFAULT_NAMESPACE) {
                NablaCore(
                    DEFAULT_NAMESPACE,
                    defaultAppContext ?: throw NablaException.Configuration.MissingContext,
                    NablaCoreConfig(
                        publicApiKey = defaultPublicApiKey ?: throw NablaException.Configuration.MissingApiKey,
                        sessionTokenProvider = defaultSessionTokenProvider ?: throw NablaException.Configuration.MissingSessionTokenProvider,
                    )
                )
            }
        }

        fun getInstance(name: String): NablaCore {
            return synchronized(this) {
                instances.getValue(name)
            }
        }

        fun initialize(context: Context, nablaCoreConfig: NablaCoreConfig): NablaCore {
            return initialize(context, nablaCoreConfig, DEFAULT_NAMESPACE)
        }

        fun initialize(context: Context, nablaCoreConfig: NablaCoreConfig, name: String): NablaCore {
            return lazyInitialize(name) {
                NablaCore(name, context, nablaCoreConfig)
            }
        }

        private fun lazyInitialize(name: String, lazyBuilder: () -> NablaCore): NablaCore {
            return synchronized(this) {
                instances.getOrPut(name) {
                    lazyBuilder()
                }
            }
        }
    }
}
