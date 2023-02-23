package com.nabla.sdk.core.domain.helper

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.boundary.SessionClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

@NablaInternal
public object AuthHelper {
    @NablaInternal
    public fun <T> Flow<T>.throwOnStartIfNotAuthenticatable(sessionClient: SessionClient): Flow<T> = onStart {
        sessionClient.authenticatableOrThrow()
    }
}
