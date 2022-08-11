package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.graphql.type.SdkModule

internal interface DeviceRepository {
    fun sendDeviceInfoAsync(activeModules: List<SdkModule>)
}
