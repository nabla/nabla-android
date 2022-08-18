package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.ModuleType

internal interface DeviceRepository {
    fun sendDeviceInfoAsync(activeModules: List<ModuleType>)
}
