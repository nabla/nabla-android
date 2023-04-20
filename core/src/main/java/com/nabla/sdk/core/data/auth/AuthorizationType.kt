package com.nabla.sdk.core.data.auth

import okhttp3.Request

internal enum class AuthorizationType {
    ACCESS_TOKEN,
    NONE,
    ;

    companion object {
        fun fromRequest(request: Request): AuthorizationType {
            return request.tag(AuthorizationType::class.java) ?: ACCESS_TOKEN
        }
    }
}
