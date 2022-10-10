package com.nabla.sdk.messagingsampleapp

import android.app.Application
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.messaging.core.NablaMessagingModule
import com.nabla.sdk.videocall.NablaVideoCallModule

internal class MessagingSampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NablaClient.initialize(
            modules = listOf(NablaMessagingModule(), NablaVideoCallModule())
        )
        NablaClient.getInstance().authenticate(
            userId = "dummy-user-id",
            sessionTokenProvider = {
                // Emulate a call to authenticate the user on your server
                // In your app, you need to replace this with an actual call to your backend to get fresh tokens
                Result.success(
                    AuthTokens(
                        refreshToken = "dummy-refresh-token",
                        accessToken = "dummy-access-token",
                    )
                )
            }
        )
    }
}