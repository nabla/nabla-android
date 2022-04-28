package com.nabla.sdk.core

import android.annotation.SuppressLint
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.toId
import com.nabla.sdk.core.injection.CoreContainer

class NablaCore constructor(
    private val name: String,
    private val nablaCoreConfig: NablaCoreConfig,
) {

    private val coreContainerDelegate = lazy {
        CoreContainer(name, nablaCoreConfig)
    }
    internal val coreContainer by coreContainerDelegate

    suspend fun authenticate(userId: String): Result<Unit> {
        return coreContainer.loginInteractor().invoke(userId.toId())
    }

    companion object {
        private const val DEFAULT_NAMESPACE = "nabla-core"

        @SuppressLint("StaticFieldLeak")
        private var defaultSingletonInstance: NablaCore? = null

        fun getInstance(): NablaCore {
            return defaultSingletonInstance ?: throw NablaException.Configuration.MissingInitialize
        }

        fun initialize(nablaCoreConfig: NablaCoreConfig): NablaCore {
            return initialize(nablaCoreConfig, DEFAULT_NAMESPACE)
        }

        fun initialize(nablaCoreConfig: NablaCoreConfig, name: String): NablaCore {
            return getOrPut(name) {
                NablaCore(name, nablaCoreConfig)
            }
        }

        private fun getOrPut(name: String, lazyBuilder: () -> NablaCore): NablaCore {
            synchronized(this) {
                return if (name == DEFAULT_NAMESPACE) {
                    // Only store default instance
                    defaultSingletonInstance ?: lazyBuilder().also { defaultSingletonInstance = it }
                } else {
                    lazyBuilder()
                }
            }
        }
    }
}
