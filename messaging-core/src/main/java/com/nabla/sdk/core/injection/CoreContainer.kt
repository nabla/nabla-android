package com.nabla.sdk.core.injection

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.ws.DefaultWebSocketEngine
import com.benasher44.uuid.Uuid
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.data.apollo.ApolloFactory
import com.nabla.sdk.core.data.auth.AuthService
import com.nabla.sdk.core.data.auth.AuthorizationInterceptor
import com.nabla.sdk.core.data.auth.PublicApiKeyInterceptor
import com.nabla.sdk.core.data.auth.SdkVersionInterceptor
import com.nabla.sdk.core.data.auth.SessionClientImpl
import com.nabla.sdk.core.data.auth.TokenLocalDataSource
import com.nabla.sdk.core.data.auth.TokenRemoteDataSource
import com.nabla.sdk.core.data.auth.UserHeaderInterceptor
import com.nabla.sdk.core.data.exception.BaseExceptionMapper
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.data.file.FileService
import com.nabla.sdk.core.data.file.FileUploadRepositoryImpl
import com.nabla.sdk.core.data.logger.AndroidLogger
import com.nabla.sdk.core.data.logger.HttpLoggingInterceptorFactory
import com.nabla.sdk.core.data.logger.SwitchableLogger
import com.nabla.sdk.core.data.patient.LocalPatientDataSource
import com.nabla.sdk.core.data.patient.PatientRepositoryImpl
import com.nabla.sdk.core.data.patient.SessionLocalDataCleanerImpl
import com.nabla.sdk.core.domain.boundary.FileUploadRepository
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.domain.boundary.SessionLocalDataCleaner
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.core.domain.interactor.LoginInteractor
import com.nabla.sdk.core.domain.interactor.LogoutInteractor
import com.nabla.sdk.messaging.core.BuildConfig
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

internal class CoreContainer(
    name: String,
    configuration: Configuration,
) {

    val logger: Logger = SwitchableLogger(
        overriddenLogger ?: AndroidLogger(),
        configuration.isLoggingEnabled
    )

    val clock: Clock = overriddenClock ?: Clock.System
    val uuidGenerator: UuidGenerator = overriddenUuidGenerator ?: object : UuidGenerator {
        override fun generate(): Uuid = Uuid.randomUUID()
    }

    private val kvStorage = configuration.context.getSharedPreferences(
        "nabla_kv_$name.sp", Context.MODE_PRIVATE
    )

    val exceptionMapper: NablaExceptionMapper = NablaExceptionMapper().apply {
        registerMapper(BaseExceptionMapper())
    }

    private val tokenRepositoryLazy = lazy {
        SessionClientImpl(
            tokenLocalDataSource,
            tokenRemoteDataSource,
            patientRepository,
            logger,
            exceptionMapper,
        )
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .apply { configuration.additionalHeadersProvider?.let { addInterceptor(UserHeaderInterceptor(it)) } }
            .addInterceptor(AuthorizationInterceptor(logger, tokenRepositoryLazy))
            .addInterceptor(PublicApiKeyInterceptor(configuration.publicApiKey))
            .addInterceptor(SdkVersionInterceptor(BuildConfig.VERSION_NAME))
            .addInterceptor(HttpLoggingInterceptorFactory.make(logger))
            .apply { overriddenOkHttpClient?.let { it(this) } }
            .build()
    }

    val apolloClient by lazy {
        ApolloFactory
            .configureBuilder(
                normalizedCacheFactory = SqlNormalizedCacheFactory(
                    configuration.context,
                    "nabla_cache_apollo_$name.db"
                )
            ).serverUrl(configuration.baseUrl + "v1/patient/graphql/sdk/authenticated")
            .httpEngine(DefaultHttpEngine(okHttpClient))
            .apply {
                val overriddenApolloWsConfig = overriddenApolloWsConfig
                if (overriddenApolloWsConfig != null) {
                    overriddenApolloWsConfig(this)
                } else {
                    webSocketEngine(DefaultWebSocketEngine(okHttpClient))
                }
            }
            .build()
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

    val sessionClient: SessionClient by tokenRepositoryLazy
    private val localPatientDataSource = LocalPatientDataSource(kvStorage)

    private val patientRepository: PatientRepository = PatientRepositoryImpl(localPatientDataSource)
    val fileUploadRepository: FileUploadRepository = FileUploadRepositoryImpl(fileService, configuration.context, uuidGenerator)

    private val sessionLocalDataCleaner: SessionLocalDataCleaner = SessionLocalDataCleanerImpl(
        apolloClient,
        localPatientDataSource,
        tokenLocalDataSource,
    )

    fun logoutInteractor() = LogoutInteractor(sessionLocalDataCleaner)
    fun loginInteractor() = LoginInteractor(patientRepository, sessionClient, logoutInteractor())

    companion object {
        @VisibleForTesting
        internal var overriddenLogger: Logger? = null

        @VisibleForTesting
        internal var overriddenOkHttpClient: ((OkHttpClient.Builder) -> Unit)? = null

        @VisibleForTesting
        internal var overriddenApolloWsConfig: ((ApolloClient.Builder) -> Unit)? = null

        @VisibleForTesting
        internal var overriddenUuidGenerator: UuidGenerator? = null

        @VisibleForTesting
        internal var overriddenClock: Clock? = null
    }
}
