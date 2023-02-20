package com.nabla.sdk.scheduling

import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.SchedulingModule
import com.nabla.sdk.core.domain.entity.ConfigurationException

public interface NablaSchedulingModule {
    public companion object Factory {
        public operator fun invoke(): SchedulingModule.Factory =
            SchedulingModule.Factory { coreContainer ->
                SchedulingModuleImpl(coreContainer)
            }
    }
}

private val NablaClient.schedulingModuleImpl: SchedulingModuleImpl
    get() = coreContainer.schedulingModule as? SchedulingModuleImpl
        ?: throw ConfigurationException.ModuleNotInitialized("NablaSchedulingModule")

internal val NablaClient.schedulingPrivateClient: SchedulingPrivateClient
    get() = schedulingModuleImpl

public val NablaClient.schedulingClient: NablaSchedulingClient
    get() = schedulingModuleImpl
