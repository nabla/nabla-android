package com.nabla.sdk.core.domain.boundary

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.Provider

@NablaInternal
public interface ProviderRepository {
    public suspend fun getProvider(id: Uuid): Provider
}
