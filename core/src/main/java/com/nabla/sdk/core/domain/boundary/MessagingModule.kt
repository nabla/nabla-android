package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.ModuleType

public interface MessagingModule : Module {
    public fun interface Factory : Module.Factory<MessagingModule> {
        override fun type(): ModuleType = ModuleType.MESSAGING
    }
}
