package com.nabla.sdk.core.domain.auth

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.domain.entity.AuthenticationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

@NablaInternal
public fun <T> Flow<T>.throwOnStartIfNotAuthenticated(sessionClient: SessionClient): Flow<T> = onStart {
    sessionClient.ensureAuthenticatedOrThrow()
}

@NablaInternal
public fun SessionClient.ensureAuthenticatedOrThrow() {
    if (!isSessionInitialized()) {
        throw AuthenticationException.NotAuthenticated
    }
}
