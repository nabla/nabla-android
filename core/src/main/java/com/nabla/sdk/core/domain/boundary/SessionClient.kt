package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.annotation.NablaInternal
import kotlinx.coroutines.flow.Flow

@NablaInternal
public interface SessionClient {
    public fun authenticatableOrThrow()
    public fun <T> authenticatableFlow(flow: Flow<T>): Flow<T>
    public suspend fun getFreshAccessToken(forceRefreshAccessToken: Boolean = false): String
    public fun markTokensAsInvalid()
}
