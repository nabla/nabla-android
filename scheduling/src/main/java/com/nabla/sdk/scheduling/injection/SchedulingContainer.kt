package com.nabla.sdk.scheduling.injection

import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.scheduling.data.AppointmentRepositoryImpl
import com.nabla.sdk.scheduling.data.GqlAppointmentCategoryDataSource
import com.nabla.sdk.scheduling.data.GqlAppointmentConfirmConsentsDataSource
import com.nabla.sdk.scheduling.data.GqlAppointmentDataSource
import com.nabla.sdk.scheduling.data.GqlAppointmentLocationDataSource
import com.nabla.sdk.scheduling.data.apollo.GqlMapper

internal class SchedulingContainer(coreContainer: CoreContainer) {
    val nablaExceptionMapper = coreContainer.exceptionMapper
    val sessionClient = coreContainer.sessionClient
    val eventsConnectionStateFlow = coreContainer.eventsConnectionState

    private val gqlMapper = GqlMapper(
        coreContainer.coreGqlMapper,
        coreContainer.logger,
    )
    private val gqlAppointmentDataSource = GqlAppointmentDataSource(
        coreContainer.logger,
        coreContainer.backgroundScope,
        coreContainer.apolloClient,
        gqlMapper,
        nablaExceptionMapper,
    )
    private val gqlAppointmentCategoryDataSource = GqlAppointmentCategoryDataSource(
        coreContainer.apolloClient,
        gqlMapper,
    )
    private val gqlAppointmentConfirmConsentsDataSource = GqlAppointmentConfirmConsentsDataSource(
        coreContainer.apolloClient,
        gqlMapper,
    )
    private val gqlAppointmentLocationDataSource = GqlAppointmentLocationDataSource(
        coreContainer.apolloClient,
    )

    val appointmentRepository = AppointmentRepositoryImpl(
        coreContainer.backgroundScope,
        gqlAppointmentDataSource,
        gqlAppointmentCategoryDataSource,
        gqlAppointmentConfirmConsentsDataSource,
        gqlAppointmentLocationDataSource,
    )
}
