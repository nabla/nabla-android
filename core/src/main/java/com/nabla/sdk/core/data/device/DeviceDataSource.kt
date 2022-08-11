package com.nabla.sdk.core.data.device

import android.os.Build

internal class DeviceDataSource {

    fun getDevice(): Device = Device(
        osVersion = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
        deviceModel = "${Build.MANUFACTURER} - ${Build.MODEL} (${Build.PRODUCT})",
    )

    data class Device(
        val osVersion: String,
        val deviceModel: String,
    )
}
