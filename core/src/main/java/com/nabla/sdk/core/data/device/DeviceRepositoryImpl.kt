package com.nabla.sdk.core.data.device

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.nabla.sdk.core.domain.boundary.DeviceRepository
import com.nabla.sdk.core.domain.boundary.Logger
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
) : DeviceRepository {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun sendDeviceInfoAsync(activeModules: List<SdkModule>) {
        coroutineScope.launch {
            runCatchingCancellable {
                logger.debug("Identifying current device", domain = "Device")
                val device = deviceDataSource.getDevice()
                val deviceId = apolloClient.mutation(
                    RegisterOrUpdateDeviceMutation(
                        deviceId = Optional.presentIfNotNull(installationDataSource.getInstallIdOrNull()),
                        device = DeviceInput(
                            deviceModel = device.deviceModel,
                            os = DeviceOs.ANDROID,
                            osVersion = device.osVersion,
                            codeVersion = sdkApiVersionDataSource.getSdkApiVersion(),
                            sdkModules = activeModules,
                        )
                    )
                ).execute().dataAssertNoErrors.registerOrUpdateDevice.deviceId
                installationDataSource.storeInstallId(deviceId)
            }.onFailure { exception ->
                logger.warn("Unable to identify device. This is not important and will be retried next time the app restarts.", exception)
            }
        }
    }
}
