package com.nabla.sdk.demo

import android.app.Application
import com.nabla.sdk.core.NablaCore
import com.nabla.sdk.core.NablaCoreConfig
import com.nabla.sdk.core.domain.entity.AuthTokens

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NablaCore.initialize(
            NablaCoreConfig.Builder {
                // Call to authenticate the app on your server
                Result.success(
                    AuthTokens(
                        refreshToken = "dummy-refresh-token",
                        accessToken = "dummy-access-token"
                    )
                )
            }.build()
        )
    }
}
