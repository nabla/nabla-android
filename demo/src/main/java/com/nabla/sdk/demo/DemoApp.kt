package com.nabla.sdk.demo

import android.app.Application
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.entity.AuthTokens

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NablaClient.initialize()
        NablaClient.getInstance().authenticate(
            userId = "dummy-user-id",
            sessionTokenProvider = {
                // Emulate a call to authenticate the user on your server
                // In your app, you need to replace this with an actual call to your backend to get fresh tokens
                Result.success(
                    AuthTokens(
                        refreshToken = "dummy-refresh-token",
                        accessToken = "dummy-access-token"
                    )
                )
            }
        )
    }
}
