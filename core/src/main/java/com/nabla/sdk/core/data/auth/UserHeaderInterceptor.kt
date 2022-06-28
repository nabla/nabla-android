package com.nabla.sdk.core.data.auth

import com.nabla.sdk.core.HeaderProvider
import okhttp3.Interceptor
import okhttp3.Response

internal class UserHeaderInterceptor(
    private val additionalHeaderProvider: HeaderProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .apply {
                additionalHeaderProvider.headers().forEach {
                    addHeader(it.name, it.value)
                }
            }.build()
        return chain.proceed(newRequest)
    }
}
