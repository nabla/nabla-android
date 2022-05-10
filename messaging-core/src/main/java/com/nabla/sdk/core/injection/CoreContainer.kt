package com.nabla.sdk.core.injection

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.network.okHttpClient
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.data.apollo.TypeAndUuidCacheKeyGenerator
import com.nabla.sdk.core.data.auth.AuthService
import com.nabla.sdk.core.data.auth.AuthorizationInterceptor
import com.nabla.sdk.core.data.auth.PublicApiKeyInterceptor
import com.nabla.sdk.core.data.auth.TokenLocalDataSource
import com.nabla.sdk.core.data.auth.TokenRemoteDataSource
import com.nabla.sdk.core.data.auth.TokenRepositoryImpl
import com.nabla.sdk.core.data.auth.UserHeaderInterceptor
import com.nabla.sdk.core.data.exception.BaseExceptionMapper
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.data.file.FileService
import com.nabla.sdk.core.data.file.FileUploadRepositoryImpl
import com.nabla.sdk.core.data.local.SecuredKVStorage
import com.nabla.sdk.core.data.logger.AndroidLogger
import com.nabla.sdk.core.data.logger.HttpLoggingInterceptorFactory
import com.nabla.sdk.core.data.logger.LoggerImpl
import com.nabla.sdk.core.data.patient.LocalPatientDataSource
import com.nabla.sdk.core.data.patient.PatientRepositoryImpl
import com.nabla.sdk.core.data.patient.SessionLocalDataCleanerImpl
import com.nabla.sdk.core.domain.boundary.FileUploadRepository
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.boundary.SessionLocalDataCleaner
import com.nabla.sdk.core.domain.boundary.TokenRepository
import com.nabla.sdk.core.domain.interactor.LoginInteractor
import com.nabla.sdk.core.domain.interactor.LogoutInteractor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

internal class CoreContainer(
    name: String,
    configuration: Configuration,
) {
    val logger: Logger = LoggerImpl(AndroidLogger(), configuration.isLoggingEnabled)

    private val securedKVStorage = SecuredKVStorage(configuration.context, name, logger)

    private val tokenRepositoryLazy = lazy {
        TokenRepositoryImpl(
            tokenLocalDataSource,
            tokenRemoteDataSource,
            patientRepository,
            logger
        )
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .apply { configuration.additionalHeadersProvider?.let { addInterceptor(UserHeaderInterceptor(it)) } }
            .addInterceptor(AuthorizationInterceptor(logger, tokenRepositoryLazy))
            .addInterceptor(PublicApiKeyInterceptor(configuration.publicApiKey))
            .addInterceptor(HttpLoggingInterceptorFactory.make(logger))
            .build()
    }

    val apolloClient by lazy {
        ApolloClient.Builder()
            .serverUrl(configuration.baseUrl + "v1/patient/graphql/sdk/authenticated")
            .normalizedCache(
                normalizedCacheFactory = SqlNormalizedCacheFactory(configuration.context, "nabla_cache_apollo_$name.db"),
                cacheKeyGenerator = TypeAndUuidCacheKeyGenerator
            ).okHttpClient(okHttpClient)
            .build()
    }

    val exceptionMapper: NablaExceptionMapper = NablaExceptionMapper().apply {
        registerMapper(BaseExceptionMapper())
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(configuration.baseUrl)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    private val authService: AuthService by lazy { retrofit.create(AuthService::class.java) }
    private val fileService: FileService by lazy { retrofit.create(FileService::class.java) }

    private val tokenLocalDataSource = TokenLocalDataSource()
    private val tokenRemoteDataSource by lazy { TokenRemoteDataSource(authService) }

    private val tokenRepository: TokenRepository by tokenRepositoryLazy
    private val localPatientDataSource = LocalPatientDataSource(securedKVStorage)
    private val patientRepository: PatientRepository = PatientRepositoryImpl(localPatientDataSource)
    val fileUploadRepository: FileUploadRepository = FileUploadRepositoryImpl(fileService, configuration.context)

    private val sessionLocalDataCleaner: SessionLocalDataCleaner = SessionLocalDataCleanerImpl(
        apolloClient,
        localPatientDataSource,
        tokenLocalDataSource,
    )

    fun logoutInteractor() = LogoutInteractor(sessionLocalDataCleaner)
    fun loginInteractor() = LoginInteractor(patientRepository, tokenRepository, logoutInteractor())
}
