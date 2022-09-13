package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.ModuleType

public interface SchedulingModule : Module {
    public fun interface Factory : Module.Factory<SchedulingModule> {
        override fun type(): ModuleType = ModuleType.SCHEDULING
    }
}
