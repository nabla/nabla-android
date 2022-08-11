package com.nabla.sdk.core.injection

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.ws.DefaultWebSocketEngine
import com.benasher44.uuid.Uuid
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nabla.sdk.core.BuildConfig
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.NetworkConfiguration
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.apollo.ApolloFactory
import com.nabla.sdk.core.data.auth.AcceptLanguageInterceptor
import com.nabla.sdk.core.data.auth.AuthService
import com.nabla.sdk.core.data.auth.AuthorizationInterceptor
import com.nabla.sdk.core.data.auth.PublicApiKeyInterceptor
import com.nabla.sdk.core.data.auth.SdkVersionInterceptor
import com.nabla.sdk.core.data.auth.SessionClientImpl
import com.nabla.sdk.core.data.auth.TokenLocalDataSource
import com.nabla.sdk.core.data.auth.TokenRemoteDataSource
import com.nabla.sdk.core.data.auth.UserHeaderInterceptor
import com.nabla.sdk.core.data.device.DeviceDataSource
import com.nabla.sdk.core.data.device.DeviceRepositoryImpl
import com.nabla.sdk.core.data.device.InstallationDataSource
import com.nabla.sdk.core.data.device.SdkApiVersionDataSource
import com.nabla.sdk.core.data.exception.BaseExceptionMapper
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.data.file.FileService
import com.nabla.sdk.core.data.file.FileUploadRepositoryImpl
import com.nabla.sdk.core.data.logger.HttpLoggingInterceptorFactory
import com.nabla.sdk.core.data.patient.LocalPatientDataSource
import com.nabla.sdk.core.data.patient.PatientRepositoryImpl
import com.nabla.sdk.core.data.patient.SessionLocalDataCleanerImpl
import com.nabla.sdk.core.domain.boundary.DeviceRepository
import com.nabla.sdk.core.domain.boundary.FileUploadRepository
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.MessagingModule
import com.nabla.sdk.core.domain.boundary.Module
import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.domain.boundary.SessionLocalDataCleaner
import com.nabla.sdk.core.domain.boundary.StringResolver
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.core.domain.boundary.VideoCallModule
import com.nabla.sdk.core.domain.entity.InternalException
import com.nabla.sdk.core.domain.interactor.LoginInteractor
import com.nabla.sdk.core.domain.interactor.LogoutInteractor
import com.nabla.sdk.core.graphql.type.SdkModule
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@NablaInternal
public class CoreContainer internal constructor(
    public val name: String,
    public val configuration: Configuration,
    networkConfiguration: NetworkConfiguration,
    private val modulesFactory: List<Module.Factory<out Module>>,
) {
    public val logger: Logger = configuration.logger

    public val clock: Clock = overriddenClock ?: Clock.System
    public val uuidGenerator: UuidGenerator = overriddenUuidGenerator ?: object : UuidGenerator {
        override fun generate(): Uuid = Uuid.randomUUID()
    }
    public val stringResolver: StringResolver = object : StringResolver {
        override fun resolve(resId: Int): String = configuration.context.getString(resId)
    }

    private val kvStorage = configuration.context.getSharedPreferences(
        "nabla_kv_${name.hashCode()}.sp", Context.MODE_PRIVATE
    )

    public val exceptionMapper: NablaExceptionMapper = NablaExceptionMapper().apply {
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

    public val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .apply { networkConfiguration.additionalHeadersProvider?.let { addInterceptor(UserHeaderInterceptor(it)) } }
            .writeTimeout(2.minutes.toJavaDuration())
            .readTimeout(2.minutes.toJavaDuration())
            .addInterceptor(AcceptLanguageInterceptor())
            .addInterceptor(AuthorizationInterceptor(logger, tokenRepositoryLazy))
            .addInterceptor(PublicApiKeyInterceptor(configuration.publicApiKey))
            .addInterceptor(SdkVersionInterceptor(BuildConfig.VERSION_NAME))
            .addInterceptor(HttpLoggingInterceptorFactory.make(logger))
            .apply { overriddenOkHttpClient?.let { it(this) } }
            .build()
    }

    public val apolloClient: ApolloClient by lazy {
        ApolloFactory
            .configureBuilder(
                normalizedCacheFactory = SqlNormalizedCacheFactory(
                    configuration.context,
                    "nabla_cache_apollo_$name.db"
                )
            )
            .serverUrl(networkConfiguration.baseUrl + "v1/patient/graphql/sdk/authenticated")
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
            .baseUrl(networkConfiguration.baseUrl)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    private val authService: AuthService by lazy { retrofit.create(AuthService::class.java) }
    private val fileService: FileService by lazy { retrofit.create(FileService::class.java) }

    private val tokenLocalDataSource = TokenLocalDataSource()
    private val tokenRemoteDataSource by lazy { TokenRemoteDataSource(authService) }

    public val sessionClient: SessionClient by tokenRepositoryLazy
    private val localPatientDataSource = LocalPatientDataSource(kvStorage)

    private val patientRepository: PatientRepository = PatientRepositoryImpl(localPatientDataSource)
    public val fileUploadRepository: FileUploadRepository = FileUploadRepositoryImpl(fileService, configuration.context, uuidGenerator)

    private val sessionLocalDataCleaner: SessionLocalDataCleaner = SessionLocalDataCleanerImpl(
        apolloClient,
        localPatientDataSource,
        tokenLocalDataSource,
    )

    private val deviceDataSource = DeviceDataSource()
    private val installationDataSource = InstallationDataSource(kvStorage)
    private val sdkApiVersionDataSource = SdkApiVersionDataSource()
    private val deviceRepository: DeviceRepository = DeviceRepositoryImpl(
        deviceDataSource,
        installationDataSource,
        sdkApiVersionDataSource,
        apolloClient,
        logger,
    )

    internal fun logoutInteractor() = LogoutInteractor(sessionLocalDataCleaner)
    internal fun loginInteractor() = LoginInteractor(patientRepository, deviceRepository, sessionClient, logoutInteractor(), activeModules())

    // ⚠️ When adding a new module, you need to update activeModules() function here

    public val videoCallModule: VideoCallModule? by lazy {
        modulesFactory.filterIsInstance<VideoCallModule.Factory>().firstOrNull()?.create(this)
    }

    public val messagingModule: MessagingModule? by lazy {
        modulesFactory.filterIsInstance<MessagingModule.Factory>().firstOrNull()?.create(this)
    }

    private fun activeModules(): List<SdkModule> = modulesFactory.map {
        when (it) {
            is VideoCallModule.Factory -> SdkModule.VIDEO_CALL
            is MessagingModule.Factory -> SdkModule.MESSAGING
            else -> throw InternalException(IllegalStateException("Unknown module $it"))
        }
    }

    public companion object {
        @VisibleForTesting
        public var overriddenOkHttpClient: ((OkHttpClient.Builder) -> Unit)? = null

        @VisibleForTesting
        public var overriddenApolloWsConfig: ((ApolloClient.Builder) -> Unit)? = null

        @VisibleForTesting
        public var overriddenUuidGenerator: UuidGenerator? = null

        @VisibleForTesting
        public var overriddenClock: Clock? = null
    }
}
