package com.nabla.sdk.messaging.core.injection

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nabla.sdk.auth.data.TokenRepositoryImpl
import com.nabla.sdk.auth.data.local.TokenLocalDataSource
import com.nabla.sdk.auth.data.remote.ApiAuthenticator
import com.nabla.sdk.auth.data.remote.AuthorizationInterceptor
import com.nabla.sdk.auth.data.remote.NablaService
import com.nabla.sdk.auth.data.remote.TokenRemoteDataSource
import com.nabla.sdk.auth.domain.boundary.SessionTokenProvider
import com.nabla.sdk.auth.domain.boundary.TokenRepository
import com.nabla.sdk.core.data.SecuredKVStorage
import com.nabla.sdk.messaging.core.data.ConversationRepositoryImpl
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class MessagingContainer(context: Context, sessionTokenProvider: SessionTokenProvider) {
    private val securedKVStorage = SecuredKVStorage(context)

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenRepository))
            .authenticator(ApiAuthenticator(tokenRepository))
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("https://api.nabla.com/")
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    private val nablaService: NablaService by lazy { retrofit.create(NablaService::class.java) }

    private val tokenLocalDataSource = TokenLocalDataSource(securedKVStorage)
    private val tokenRemoteDataSource by lazy { TokenRemoteDataSource(nablaService) }
    private val tokenRepository: TokenRepository by lazy {
        TokenRepositoryImpl(
            tokenLocalDataSource,
            tokenRemoteDataSource,
            sessionTokenProvider
        )
    }
    val conversationRepository: ConversationRepository = ConversationRepositoryImpl()
}
