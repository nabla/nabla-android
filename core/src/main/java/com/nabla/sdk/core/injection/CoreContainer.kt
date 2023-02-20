package com.nabla.sdk.core.injection

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.benasher44.uuid.Uuid
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nabla.sdk.core.BuildConfig
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.NetworkConfiguration
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.apollo.ApolloFactory
import com.nabla.sdk.core.data.apollo.ConnectionStateAwareWebsocketEngine
import com.nabla.sdk.core.data.apollo.CoreGqlMapper
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
import com.nabla.sdk.core.data.logger.ErrorReporterLogger
import com.nabla.sdk.core.data.logger.HttpLoggingInterceptorFactory
import com.nabla.sdk.core.data.logger.MutableCompositeLogger
import com.nabla.sdk.core.data.patient.LocalPatientDataSource
import com.nabla.sdk.core.data.patient.PatientRepositoryImpl
import com.nabla.sdk.core.data.patient.ProviderRepositoryImpl
import com.nabla.sdk.core.data.patient.SessionLocalDataCleanerImpl
import com.nabla.sdk.core.data.reporter.NoOpErrorReporter
import com.nabla.sdk.core.domain.boundary.DeviceRepository
import com.nabla.sdk.core.domain.boundary.ErrorReporter
import com.nabla.sdk.core.domain.boundary.ErrorReporter.Companion.reporterFactory
import com.nabla.sdk.core.domain.boundary.FileUploadRepository
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.MessagingModule
import com.nabla.sdk.core.domain.boundary.Module
import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.boundary.ProviderRepository
import com.nabla.sdk.core.domain.boundary.SchedulingModule
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.domain.boundary.SessionLocalDataCleaner
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.core.domain.boundary.VideoCallModule
import com.nabla.sdk.core.domain.entity.EventsConnectionState
import com.nabla.sdk.core.domain.entity.ModuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val modulesFactory: List<Module.Factory<out Module<*>>>,
    private val sessionTokenProvider: SessionTokenProvider,
) {
    public val logger: Logger = MutableCompositeLogger(configuration.logger)
    public val errorReporter: ErrorReporter = if (configuration.enableReporting) {
        reporterFactory?.create(logger) ?: NoOpErrorReporter()
    } else {
        NoOpErrorReporter()
    }

    public val coreGqlMapper: CoreGqlMapper = CoreGqlMapper(logger)

    public val clock: Clock = overriddenClock ?: Clock.System
    public val uuidGenerator: UuidGenerator = overriddenUuidGenerator ?: object : UuidGenerator {
        override fun generate(): Uuid = Uuid.randomUUID()
    }

    public val backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob())

    private val scopedKvStorage = configuration.context.getSharedPreferences(
        "nabla_kv_${name.hashCode()}.sp", Context.MODE_PRIVATE
    )

    private val dangerouslyUnscopedKvStorage = configuration.context.getSharedPreferences(
        "nabla_kv_global.sp", Context.MODE_PRIVATE
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
            sessionTokenProvider,
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

    private val websocketEngine = ConnectionStateAwareWebsocketEngine(
        webSocketFactory = okHttpClient,
        clock = clock,
    )

    public val eventsConnectionState: Flow<EventsConnectionState> = websocketEngine.connectionStateFlow
        .map {
            when (it) {
                ConnectionStateAwareWebsocketEngine.WebSocketConnectionState.Connected -> EventsConnectionState.Connected
                is ConnectionStateAwareWebsocketEngine.WebSocketConnectionState.Disconnected -> EventsConnectionState.Disconnected(since = it.since)
                ConnectionStateAwareWebsocketEngine.WebSocketConnectionState.NotConnected -> EventsConnectionState.NotConnected
                ConnectionStateAwareWebsocketEngine.WebSocketConnectionState.Connecting -> EventsConnectionState.Connecting
            }
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
                    webSocketEngine(websocketEngine)
                }
            }
            .apply { overriddenApolloConfig?.let { it(this) } }
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
    private val localPatientDataSource = LocalPatientDataSource(scopedKvStorage)

    internal val patientRepository: PatientRepository = PatientRepositoryImpl(localPatientDataSource)
    public val fileUploadRepository: FileUploadRepository = FileUploadRepositoryImpl(fileService, configuration.context, uuidGenerator)

    public val providerRepository: ProviderRepository by lazy {
        ProviderRepositoryImpl(apolloClient, coreGqlMapper)
    }

    internal val sessionLocalDataCleaner: SessionLocalDataCleaner = SessionLocalDataCleanerImpl(
        apolloClient,
        localPatientDataSource,
        tokenLocalDataSource,
    )

    private val deviceDataSource = DeviceDataSource()
    private val installationDataSource = InstallationDataSource(
        unscopedSharedPreferences = dangerouslyUnscopedKvStorage,
        legacyScopedStorage = scopedKvStorage
    )
    private val sdkApiVersionDataSource = SdkApiVersionDataSource()
    internal val deviceRepository: DeviceRepository by lazy {
        DeviceRepositoryImpl(
            deviceDataSource,
            installationDataSource,
            sdkApiVersionDataSource,
            apolloClient,
            logger,
            errorReporter,
            backgroundScope
        )
    }

    init {
        (logger as? MutableCompositeLogger)?.addLogger(
            ErrorReporterLogger(
                errorReporter = errorReporter,
                publicApiKey = configuration.publicApiKey,
                sdkVersion = BuildConfig.VERSION_NAME,
                phoneName = if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) { Build.MODEL } else { "${Build.MANUFACTURER} ${Build.MODEL}" },
                androidApiLevel = Build.VERSION.SDK_INT,
            )
        )
    }

    public val videoCallModule: VideoCallModule? by lazy {
        modulesFactory.filterIsInstance<VideoCallModule.Factory>().firstOrNull()?.create(this)
    }

    public val schedulingModule: SchedulingModule? by lazy {
        modulesFactory.filterIsInstance<SchedulingModule.Factory>().firstOrNull()?.create(this)
    }

    public val messagingModule: MessagingModule? by lazy {
        modulesFactory.filterIsInstance<MessagingModule.Factory>().firstOrNull()?.create(this)
    }

    internal fun activeModules(): List<ModuleType> = modulesFactory.map { it.type() }

    public companion object {
        @VisibleForTesting
        public var overriddenOkHttpClient: ((OkHttpClient.Builder) -> Unit)? = null

        @VisibleForTesting
        public var overriddenApolloWsConfig: ((ApolloClient.Builder) -> Unit)? = null

        @VisibleForTesting
        public var overriddenApolloConfig: ((ApolloClient.Builder) -> Unit)? = null

        @VisibleForTesting
        public var overriddenUuidGenerator: UuidGenerator? = null

        @VisibleForTesting
        public var overriddenClock: Clock? = null
    }
}
