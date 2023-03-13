package com.nabla.sdk.messagingsampleapp

import android.app.Application
import android.util.Log
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.entity.AccessToken
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.AuthenticationException
import com.nabla.sdk.core.domain.entity.RefreshToken
import com.nabla.sdk.messaging.core.NablaMessagingModule
import com.nabla.sdk.videocall.NablaVideoCallModule

internal class MessagingSampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NablaClient.initialize(
            modules = listOf(NablaMessagingModule(), NablaVideoCallModule()),
            sessionTokenProvider = {
                // Emulate a call to authenticate the user on your server
                // In your app, you need to replace this with an actual call to your backend to get fresh tokens
                Result.success(
                    AuthTokens(
                        AccessToken("dummy-access-token"),
                        RefreshToken("dummy-refresh-token"),
                    )
                )
            }
        )

        try {
            NablaClient.getInstance().setCurrentUserOrThrow(userId = "dummy-user-id")
        } catch (e: AuthenticationException.CurrentUserAlreadySet) {
            Log.w("MessagingSampleApp", "Current user already set", e)
        }
    }
}
