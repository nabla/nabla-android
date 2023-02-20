package com.nabla.sdk.core.domain.boundary

internal interface SessionLocalDataCleaner {
    suspend fun cleanLocalSessionData()
}
