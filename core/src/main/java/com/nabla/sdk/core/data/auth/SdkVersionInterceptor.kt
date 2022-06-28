package com.nabla.sdk.core.data.auth

import okhttp3.Interceptor
import okhttp3.Response

internal class SdkVersionInterceptor(private val sdkVersion: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .addHeader("X-Nabla-SDK-Platform", "android")
            .addHeader("X-Nabla-SDK-Version", sdkVersion)
            .build()
        return chain.proceed(newRequest)
    }
}
