package com.nabla.sdk.core.data.device

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.nabla.sdk.core.domain.boundary.DeviceRepository
import com.nabla.sdk.core.domain.boundary.ErrorReporter
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.ModuleType
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

    override fun sendDeviceInfoAsync(activeModules: List<ModuleType>) {
        coroutineScope.launch {
            runCatchingCancellable {
                logger.debug("Identifying current device", domain = "Device")
                val device = deviceDataSource.getDevice()
                val gqlActiveModules = activeModules.map {
                    when (it) {
                        ModuleType.VIDEO_CALL -> SdkModule.VIDEO_CALL
                        ModuleType.MESSAGING -> SdkModule.MESSAGING
                        ModuleType.SCHEDULING -> SdkModule.VIDEO_CALL_SCHEDULING
                    }
                }
                apolloClient.mutation(
                    RegisterOrUpdateDeviceMutation(
                        deviceId = Optional.presentIfNotNull(installationDataSource.getInstallIdOrNull()),
                        device = DeviceInput(
                            deviceModel = device.deviceModel,
                            os = DeviceOs.ANDROID,
                            osVersion = device.osVersion,
                            codeVersion = sdkApiVersionDataSource.getSdkApiVersion(),
                            sdkModules = gqlActiveModules,
                        )
                    )
                ).execute().dataAssertNoErrors.registerOrUpdateDevice.let {
                    installationDataSource.storeInstallId(it.deviceId)
                    val sentry = it.sentry
                    if (sentry != null) {
                        errorReporter.enable(dsn = sentry.dsn, env = sentry.env)
                    } else {
                        errorReporter.disable()
                    }
                }
            }.onFailure { exception ->
                logger.warn("Unable to identify device. This is not important and will be retried next time the app restarts.", exception)
            }
        }
    }
}
