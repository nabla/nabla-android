package com.nabla.sdk.scheduling

import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.SchedulingModule
import com.nabla.sdk.core.domain.entity.ConfigurationException

public interface NablaSchedulingModule : SchedulingModule {
    public companion object Factory {
        public operator fun invoke(): SchedulingModule.Factory =
            SchedulingModule.Factory { coreContainer ->
                NablaSchedulingClientImpl(coreContainer)
            }
    }
}

internal val NablaClient.schedulingModule: SchedulingModule
    get() = coreContainer.schedulingModule ?: throw moduleNotInitialized

internal val NablaClient.schedulingInternalModule: SchedulingInternalModule
    get() = coreContainer.schedulingModule as? SchedulingInternalModule
        ?: throw moduleNotInitialized

internal val NablaClient.schedulingClient: NablaSchedulingClient
    get() = coreContainer.schedulingModule as? NablaSchedulingClient
        ?: throw moduleNotInitialized

private val moduleNotInitialized = ConfigurationException.ModuleNotInitialized("NablaSchedulingModule")
