package com.nabla.sdk.core.data.device

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.apollo.dataOrThrowOnError
import com.nabla.sdk.core.data.exception.GraphQLException
import com.nabla.sdk.core.domain.boundary.DeviceRepository
import com.nabla.sdk.core.domain.boundary.ErrorReporter
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.ModuleType
import com.nabla.sdk.core.domain.entity.StringId
import com.nabla.sdk.core.graphql.RegisterOrUpdateDeviceMutation
import com.nabla.sdk.core.graphql.type.DeviceInput
import com.nabla.sdk.core.graphql.type.DeviceOs
import com.nabla.sdk.core.graphql.type.SdkModule
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class DeviceRepositoryImpl(
    private val deviceDataSource: DeviceDataSource,
    private val installationDataSource: InstallationDataSource,
    private val sdkApiVersionDataSource: SdkApiVersionDataSource,
    private val apolloClient: ApolloClient,
    private val logger: Logger,
    private val errorReporter: ErrorReporter,
) : DeviceRepository {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun sendDeviceInfoAsync(activeModules: List<ModuleType>, userId: StringId) {
        coroutineScope.launch {
            logger.debug("Identifying current device", domain = LOG_DOMAIN)
            val device = deviceDataSource.getDevice()
            val gqlActiveModules = activeModules.map {
                when (it) {
                    ModuleType.VIDEO_CALL -> SdkModule.VIDEO_CALL
                    ModuleType.MESSAGING -> SdkModule.MESSAGING
                    ModuleType.SCHEDULING -> SdkModule.VIDEO_CALL_SCHEDULING
                }
            }

            val deviceInput = DeviceInput(
                deviceModel = device.deviceModel,
                os = DeviceOs.ANDROID,
                osVersion = device.osVersion,
                codeVersion = sdkApiVersionDataSource.getSdkApiVersion(),
                sdkModules = gqlActiveModules,
            )

            runCatchingCancellable {
                val installId = installationDataSource.getInstallIdOrNull(userId)
                registerOrUpdateDeviceOp(installId, deviceInput)
            }
                .fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { throwable ->
                        if (throwable is GraphQLException && throwable.numericCode in errorsToRegisterNewDevice) {
                            logger.info("Recoverable device update failure: will register a new one", throwable, domain = LOG_DOMAIN)
                            runCatchingCancellable {
                                registerOrUpdateDeviceOp(installId = null, deviceInput)
                            }
                        } else Result.failure(throwable)
                    }
                )
                .onSuccess {
                    with(it.registerOrUpdateDevice) {
                        logger.debug("Device $deviceId registered/updated successfully", domain = LOG_DOMAIN)
                        installationDataSource.storeInstallId(deviceId, userId)
                        if (sentry != null) {
                            errorReporter.enable(dsn = sentry.dsn, env = sentry.env)
                        } else {
                            errorReporter.disable()
                        }
                    }
                }
                .onFailure { exception ->
                    logger.warn(
                        "Unable to identify device. This is not important and will be retried next time the app restarts.",
                        exception,
                        domain = LOG_DOMAIN,
                    )
                }
        }
    }

    private suspend fun registerOrUpdateDeviceOp(installId: Uuid?, deviceInput: DeviceInput): RegisterOrUpdateDeviceMutation.Data {
        logger.debug(
            message = if (installId == null) "Registering new device" else "Updating device with id $installId",
            domain = LOG_DOMAIN,
        )

        return apolloClient.mutation(
            RegisterOrUpdateDeviceMutation(
                deviceId = Optional.presentIfNotNull(installId),
                device = deviceInput,
            )
        ).execute().dataOrThrowOnError
    }

    companion object {
        private const val LOG_DOMAIN = "Device"
        private const val ERROR_ENTITY_NOT_FOUND = 20_000
        private const val ERROR_DEVICE_BELONGS_TO_ANOTHER_USER = 48_000
        private val errorsToRegisterNewDevice = listOf(ERROR_ENTITY_NOT_FOUND, ERROR_DEVICE_BELONGS_TO_ANOTHER_USER)
    }
}
