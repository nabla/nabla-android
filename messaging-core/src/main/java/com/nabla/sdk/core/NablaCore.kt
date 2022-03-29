package com.nabla.sdk.core

import android.content.Context
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.injection.CoreContainer

class NablaCore private constructor() {

    private lateinit var appContext: Context
    private lateinit var sessionTokenProvider: SessionTokenProvider
    private val coreContainer by lazy { CoreContainer(appContext, sessionTokenProvider) }

    fun init(appContext: Context, sessionTokenProvider: SessionTokenProvider) {
        this.appContext = appContext.applicationContext
        this.sessionTokenProvider = sessionTokenProvider
    }

    suspend fun authenticate(userId: String) {
        coreContainer.loginInteractor().invoke(userId)
    }

    companion object {
        val instance = NablaCore()
    }
}
