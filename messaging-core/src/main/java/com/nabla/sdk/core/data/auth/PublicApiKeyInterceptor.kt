package com.nabla.sdk.core.data.auth

import okhttp3.Interceptor
import okhttp3.Response

internal class PublicApiKeyInterceptor(private val publicApiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .addHeader("X-Nabla-API-Key", publicApiKey)
            .build()
        return chain.proceed(newRequest)
    }
}
