package com.nabla.sdk.scheduling.injection

import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.scheduling.data.AppointmentRepositoryImpl
import com.nabla.sdk.scheduling.data.GqlAppointmentCategoryDataSource
import com.nabla.sdk.scheduling.data.GqlAppointmentConfirmConsentsDataSource
import com.nabla.sdk.scheduling.data.GqlAppointmentDataSource
import com.nabla.sdk.scheduling.data.GqlAppointmentLocationDataSource
import com.nabla.sdk.scheduling.data.apollo.GqlMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class SchedulingContainer(private val coreContainer: CoreContainer) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gqlMapper = GqlMapper(
        coreContainer.coreGqlMapper,
    )
    private val gqlAppointmentDataSource = GqlAppointmentDataSource(
        coreContainer.logger,
        repoScope,
        coreContainer.apolloClient,
        gqlMapper,
    )
    private val gqlAppointmentCategoryDataSource = GqlAppointmentCategoryDataSource(
        coreContainer.apolloClient,
        gqlMapper,
    )
    private val gqlAppointmentConfirmConsentsDataSource = GqlAppointmentConfirmConsentsDataSource(
        coreContainer.apolloClient,
        gqlMapper
    )
    private val gqlAppointmentLocationDataSource = GqlAppointmentLocationDataSource(
        coreContainer.apolloClient,
        gqlMapper
    )

    val appointmentRepository = AppointmentRepositoryImpl(
        repoScope,
        gqlAppointmentDataSource,
        gqlAppointmentCategoryDataSource,
        gqlAppointmentConfirmConsentsDataSource,
        gqlAppointmentLocationDataSource,
    )
    val nablaExceptionMapper = coreContainer.exceptionMapper
    val sessionClient = coreContainer.sessionClient
}
