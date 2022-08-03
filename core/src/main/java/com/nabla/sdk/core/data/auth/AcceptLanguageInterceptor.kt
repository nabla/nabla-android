package com.nabla.sdk.core.data.auth

import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

internal class AcceptLanguageInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .addHeader("Accept-Language", Locale.getDefault().toLanguageTag())
            .build()
        return chain.proceed(newRequest)
    }
}
