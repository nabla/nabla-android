package com.nabla.sdk.core

import android.content.Context
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.injection.CoreContainer

class NablaCore private constructor() {

    private lateinit var appContext: Context
    private lateinit var sessionTokenProvider: SessionTokenProvider
    private var nablaCoreConfig: NablaCoreConfig = NablaCoreConfig()

    private val coreContainerDelegate = lazy {
        CoreContainer(appContext, sessionTokenProvider, nablaCoreConfig)
    }
    private val coreContainer by coreContainerDelegate

    val conversationRepository by lazy { coreContainer.conversationRepository }

    // Private init, done by app startup
    internal fun init(appContext: Context) {
        this.appContext = appContext.applicationContext
    }

    // Public init, done by client
    fun init(sessionTokenProvider: SessionTokenProvider) {
        this.sessionTokenProvider = sessionTokenProvider
    }

    // Optional config
    fun setConfig(nablaCoreConfig: NablaCoreConfig) {
        check(!coreContainerDelegate.isInitialized()) {
            "Cannot change/set config after first usage"
        }
        this.nablaCoreConfig = nablaCoreConfig
    }

    suspend fun authenticate(userId: String) {
        coreContainer.loginInteractor().invoke(userId)
    }

    companion object {
        val instance = NablaCore()
    }
}
