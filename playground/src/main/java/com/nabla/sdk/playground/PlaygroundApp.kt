package com.nabla.sdk.playground

import android.app.Application
import com.nabla.sdk.core.NablaCore
import com.nabla.sdk.core.NablaCoreConfig
import com.nabla.sdk.core.domain.entity.AuthTokens

class PlaygroundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NablaCore.instance.setConfig(NablaCoreConfig(isLoggingEnable = true))
        NablaCore.instance.init {
            // Call to authenticate the app on client server

            // TODO mocks
            Result.success(
                AuthTokens(
                    refreshToken = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MTcxMjQ4MDYzNSwiaWF0IjoxNjQ5MzIyMjM1fQ.qelpAZqH4RrAR7u6yEy6_71iNqhSfzYAJAshGsQzq2c",
                    accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MTY4MDg1ODIzNSwiaWF0IjoxNjQ5MzIyMjM1fQ.UWnYy4Bxa2BvH58SgzkCF8aXG-lx8KVpU_TDXOFXdZ4"
                )
            )
        }
    }
}
