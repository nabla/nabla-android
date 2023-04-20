package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.ModuleType
import com.nabla.sdk.core.injection.CoreContainer

public interface Module<InternalClient> {

    @NablaInternal
    public val internalClient: InternalClient

    public interface Factory<M : Module<*>> {
        @NablaInternal
        public fun create(coreContainer: CoreContainer): M

        @NablaInternal
        public fun type(): ModuleType
    }
}
