package com.nabla.sdk.core.domain.helper

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.boundary.SessionClient
import kotlinx.coroutines.flow.Flow

@NablaInternal
public object AuthHelper {
    @NablaInternal
    public fun <T> Flow<T>.authenticatable(sessionClient: SessionClient): Flow<T> = sessionClient.authenticatableFlow(this)
}
