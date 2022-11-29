package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.ModuleType
import com.nabla.sdk.core.domain.entity.StringId

internal interface DeviceRepository {
    fun sendDeviceInfoAsync(activeModules: List<ModuleType>, userId: StringId)
}
