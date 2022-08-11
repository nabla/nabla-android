package com.nabla.sdk.core.data.device

import com.nabla.sdk.core.BuildConfig

internal class SdkApiVersionDataSource {
    fun getSdkApiVersion(): Int = BuildConfig.API_VERSION
}
