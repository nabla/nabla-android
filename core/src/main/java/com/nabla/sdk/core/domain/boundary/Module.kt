package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.ModuleType
import com.nabla.sdk.core.injection.CoreContainer

public interface Module {
    public interface Factory<T : Module> {
        @NablaInternal
        public fun create(coreContainer: CoreContainer): T
        @NablaInternal
        public fun type(): ModuleType
    }
}
